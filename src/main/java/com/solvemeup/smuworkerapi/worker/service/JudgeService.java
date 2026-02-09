package com.solvemeup.smuworkerapi.worker.service;

import com.solvemeup.smuworkerapi.worker.Entity.ProblemMetadata;
import com.solvemeup.smuworkerapi.worker.Entity.TestCase;
import com.solvemeup.smuworkerapi.worker.dto.JudgeRequestMessage;
import com.solvemeup.smuworkerapi.worker.dto.JudgeResponseMessage;
import com.solvemeup.smuworkerapi.worker.enums.JudgeResult;
import com.solvemeup.smuworkerapi.worker.message.JudgeResultProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeService {

    private final MetadataLoader metadataLoader;
    private final TestCaseLoader testCaseLoader;
    private final CodeGenerator codeGenerator;
    private final DockerExecutor dockerExecutor;
    private final OutputComparator outputComparator;
    private final JudgeResultProducer resultProducer;

    public void judge(JudgeRequestMessage request) {
        log.info("Starting judge - judgeId: {}, problemId: {}, submissionId: {}, language: {}",
                request.judgeId(), request.problemId(), request.submissionId(), request.language());

        try {
            ProblemMetadata metadata = metadataLoader.loadMetadata(request.problemId());

            List<TestCase> testCases = testCaseLoader.loadTestCases(request.problemId());

            if (testCases.isEmpty()) {
                log.error("No test cases found for problem {}", request.problemId());
                sendErrorResult(request, JudgeResult.SYSTEM_ERROR, 0, 0);
                return;
            }

            log.info("Loaded {} test cases for problem {}", testCases.size(), request.problemId());

            String executableCode = codeGenerator.generateExecutableCode(
                    request.language(),
                    request.sourceCode(),
                    metadata
            );

            JudgeResult finalResult = JudgeResult.AC;
            int totalTimeUsed = 0;
            int maxMemoryUsed = 0;

            for (TestCase testCase : testCases) {
                log.info("Executing test case {}/{} - judgeId: {}",
                        testCase.number(), testCases.size(), request.judgeId());

                DockerExecutor.ExecutionResult execResult = dockerExecutor.execute(
                        request.language(),
                        executableCode,
                        testCase.input(),
                        testCase.number(),
                        request.timeLimitMillis(),
                        request.memoryLimitMegabytes()
                );

                if (execResult.status() == JudgeResult.AC) {
                    boolean isCorrect = outputComparator.compare(
                            execResult.output(),
                            testCase.expectedOutut()
                    );

                    if (!isCorrect) {
                        log.error("=== Wrong Answer at test case {} ===", testCase.number());
                        log.error("Expected: [{}]", testCase.expectedOutut());
                        log.error("Actual:   [{}]", execResult.output());

                        log.info("Wrong answer at test case {} - judgeId: {}", testCase.number(), request.judgeId());
                        finalResult = JudgeResult.WA;
                        break;
                    }
                }
                totalTimeUsed += execResult.executionTimeMillis();
                maxMemoryUsed = Math.max(maxMemoryUsed, execResult.memoryUsageKB());

                log.info("Test case {}/{} executed - status: {}, time: {}ms, memory: {}KB",
                        testCase.number(), testCases.size(),
                        execResult.status(), execResult.executionTimeMillis(), execResult.memoryUsageKB());

                if (execResult.status() == JudgeResult.AC) {
                    boolean isCorrect = outputComparator.compare(
                            execResult.output(),
                            testCase.expectedOutut()
                    );

                    if (!isCorrect) {
                        log.info("Wrong answer at test case {} - judgeId: {}", testCase.number(), request.judgeId());
                        finalResult = JudgeResult.WA;
                        break;
                    }
                } else {
                    log.info("Execution failed at test case {} with status {} - judgeId: {}",
                            testCase.number(), execResult.status(), request.judgeId());
                    finalResult = execResult.status();

                    if (execResult.error() != null && !execResult.error().isEmpty()) {
                        log.debug("Error output:\n{}", execResult.error());
                    }
                    break;
                }

                log.info("Test case {}/{} passed - judgeId: {}",
                        testCase.number(), testCases.size(), request.judgeId());
            }

            int memoryUsedMB = maxMemoryUsed / 1024;

            resultProducer.sendResult(new JudgeResponseMessage(
                    request.judgeId(),
                    request.submissionId(),
                    finalResult,
                    totalTimeUsed,
                    memoryUsedMB
            ));

            log.info("Judge completed - judgeId: {}, result: {}, time: {}ms, memory: {}MB",
                    request.judgeId(), finalResult, totalTimeUsed, memoryUsedMB);

        } catch (Exception e) {
            log.error("Judge failed with exception - judgeId: {}", request.judgeId(), e);
            sendErrorResult(request, JudgeResult.SYSTEM_ERROR, 0, 0);
        }
    }

    private void sendErrorResult(JudgeRequestMessage request, JudgeResult result, int timeUsed, int memoryUsed) {
        resultProducer.sendResult(new JudgeResponseMessage(
                request.judgeId(),
                request.submissionId(),
                result,
                timeUsed,
                memoryUsed
        ));
    }
}