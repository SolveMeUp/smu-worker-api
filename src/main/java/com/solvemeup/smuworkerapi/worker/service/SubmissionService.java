package com.solvemeup.smuworkerapi.worker.service;

import com.solvemeup.smuworkerapi.worker.Entity.TestCase;
import com.solvemeup.smuworkerapi.worker.dto.SubmissionRequestMessage;
import com.solvemeup.smuworkerapi.worker.dto.SubmissionResultMessage;
import com.solvemeup.smuworkerapi.worker.enums.JudgeResult;
import com.solvemeup.smuworkerapi.worker.message.SubmissionResultProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final TestCaseLoader testCaseLoader;
    private final CodeGenerator codeGenerator;
    private final DockerExecutor dockerExecutor;
    private final OutputComparator outputComparator;
    private final ArgumentConverter argumentConverter;
    private final SubmissionResultProducer resultProducer;

    public void judge(SubmissionRequestMessage request) {
        log.info("Starting submission - submissionId: {}, problemId: {}, language: {}",
                request.submissionId(), request.problemId(), request.language());

        try {
            List<TestCase> testCases = testCaseLoader.loadTestCases(request.problemId());

            if (testCases.isEmpty()) {
                log.error("No test cases found for problem {}", request.problemId());
                sendErrorResult(request, JudgeResult.SYSTEM_ERROR);
                return;
            }

            log.info("Loaded {} test cases for problem {}", testCases.size(), request.problemId());

            String executableCode = codeGenerator.generateBatchExecutableCode(
                    request.language(),
                    request.sourceCode(),
                    request.functionName(),
                    request.parameters(),
                    request.returnType()
            );

            List<String> caseInputs = testCases.stream().map(TestCase::input).toList();
            DockerExecutor.BatchExecutionResult batch = dockerExecutor.executeBatch(
                    request.language(),
                    executableCode,
                    caseInputs,
                    request.timeLimitMillis(),
                    request.memoryLimitKilobytes()
            );

            JudgeResult finalResult = JudgeResult.AC;
            int maxTimeUsed = 0;
            int maxMemoryUsed = 0;

            Integer failedCaseIndex = null;
            String failedExpectedOutput = null;
            String failedActualOutput = null;
            String failedArguments = null;

            for (int i = 0; i < testCases.size(); i++) {
                TestCase testCase = testCases.get(i);
                DockerExecutor.BatchCaseResult caseResult = batch.cases().get(i);
                int caseIndex = testCase.number() - 1;

                maxTimeUsed = Math.max(maxTimeUsed, caseResult.executionTimeMillis());
                maxMemoryUsed = Math.max(maxMemoryUsed, caseResult.memoryUsageKB());

                if (caseResult.status() == JudgeResult.CE) {
                    finalResult = JudgeResult.CE;
                    log.info("Compile error - submissionId: {}", request.submissionId());
                    break;
                }

                if (caseResult.status() != JudgeResult.AC) {
                    finalResult = caseResult.status();
                    failedCaseIndex = caseIndex;
                    failedExpectedOutput = testCase.expectedOutput();
                    failedActualOutput = caseResult.output();
                    failedArguments = argumentConverter.stdinToArgumentsJson(testCase.input(), request.parameters());
                    log.info("Execution failed at test case {} with status {} - submissionId: {}",
                            testCase.number(), caseResult.status(), request.submissionId());
                    break;
                }

                boolean isCorrect = outputComparator.compare(
                        caseResult.output(), testCase.expectedOutput(), request.returnType());
                if (!isCorrect) {
                    finalResult = JudgeResult.WA;
                    failedCaseIndex = caseIndex;
                    failedExpectedOutput = testCase.expectedOutput();
                    failedActualOutput = caseResult.output();
                    failedArguments = argumentConverter.stdinToArgumentsJson(testCase.input(), request.parameters());
                    log.info("Wrong answer at test case {} - submissionId: {}", testCase.number(), request.submissionId());
                    break;
                }
            }

            resultProducer.sendResult(new SubmissionResultMessage(
                    request.submissionId(),
                    finalResult,
                    failedCaseIndex,
                    failedArguments,
                    failedExpectedOutput,
                    failedActualOutput,
                    maxTimeUsed,
                    maxMemoryUsed
            ));

            log.info("Submission completed - submissionId: {}, verdict: {}, time: {}ms, memory: {}KB",
                    request.submissionId(), finalResult, maxTimeUsed, maxMemoryUsed);

        } catch (Exception e) {
            log.error("Submission failed with exception - submissionId: {}", request.submissionId(), e);
            sendErrorResult(request, JudgeResult.SYSTEM_ERROR);
        }
    }

    private void sendErrorResult(SubmissionRequestMessage request, JudgeResult result) {
        resultProducer.sendResult(new SubmissionResultMessage(
                request.submissionId(),
                result,
                null, null, null, null, null, null
        ));
    }
}
