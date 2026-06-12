package com.solvemeup.smuworkerapi.worker.service;

import com.solvemeup.smuworkerapi.worker.dto.ExecutionRequestMessage;
import com.solvemeup.smuworkerapi.worker.dto.ExecutionResultMessage;
import com.solvemeup.smuworkerapi.worker.dto.ExecutionTestCase;
import com.solvemeup.smuworkerapi.worker.enums.JudgeResult;
import com.solvemeup.smuworkerapi.worker.message.ExecutionResultProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final CodeGenerator codeGenerator;
    private final DockerExecutor dockerExecutor;
    private final OutputComparator outputComparator;
    private final ArgumentConverter argumentConverter;
    private final ExecutionResultProducer resultProducer;

    public void execute(ExecutionRequestMessage request) {
        log.info("Starting execution - executionId: {}, problemId: {}, language: {}, cases: {}",
                request.executionId(), request.problemId(), request.language(), request.testCases().size());

        try {
            String executableCode = codeGenerator.generateExecutableCode(
                    request.language(),
                    request.sourceCode(),
                    request.functionName(),
                    request.parameters(),
                    request.returnType()
            );

            boolean ceDetected = false;

            for (ExecutionTestCase testCase : request.testCases()) {
                if (ceDetected) {
                    resultProducer.sendResult(new ExecutionResultMessage(
                            request.executionId(),
                            testCase.caseIndex(),
                            JudgeResult.CE,
                            testCase.arguments(),
                            testCase.expectedOutput(),
                            null, null, null
                    ));
                    continue;
                }

                String stdin = argumentConverter.toStdinInput(
                        testCase.arguments(), request.parameters());

                DockerExecutor.ExecutionResult execResult = dockerExecutor.execute(
                        request.language(),
                        executableCode,
                        stdin,
                        testCase.caseIndex() + 1,
                        request.timeLimitMillis(),
                        request.memoryLimitKilobytes()
                );

                log.info("Run case {} executed - status: {}, time: {}ms, memory: {}KB",
                        testCase.caseIndex(), execResult.status(),
                        execResult.executionTimeMillis(), execResult.memoryUsageKB());

                if (execResult.status() == JudgeResult.CE) {
                    ceDetected = true;
                    resultProducer.sendResult(new ExecutionResultMessage(
                            request.executionId(),
                            testCase.caseIndex(),
                            JudgeResult.CE,
                            testCase.arguments(),
                            testCase.expectedOutput(),
                            null, null, null
                    ));
                    continue;
                }

                JudgeResult status = execResult.status();
                if (status == JudgeResult.AC) {
                    boolean isCorrect = outputComparator.compare(
                            execResult.output(), testCase.expectedOutput(), request.returnType());
                    status = isCorrect ? JudgeResult.AC : JudgeResult.WA;
                }

                resultProducer.sendResult(new ExecutionResultMessage(
                        request.executionId(),
                        testCase.caseIndex(),
                        status,
                        testCase.arguments(),
                        testCase.expectedOutput(),
                        execResult.output(),
                        execResult.executionTimeMillis(),
                        execResult.memoryUsageKB()
                ));
            }

            log.info("Execution completed - executionId: {}", request.executionId());

        } catch (Exception e) {
            log.error("Execution failed with exception - executionId: {}", request.executionId(), e);
            for (ExecutionTestCase testCase : request.testCases()) {
                resultProducer.sendResult(new ExecutionResultMessage(
                        request.executionId(),
                        testCase.caseIndex(),
                        JudgeResult.SYSTEM_ERROR,
                        testCase.arguments(),
                        testCase.expectedOutput(),
                        null, null, null
                ));
            }
        }
    }
}
