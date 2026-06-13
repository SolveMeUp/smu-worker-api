package com.solvemeup.smuworkerapi.worker.service;

import com.solvemeup.smuworkerapi.worker.dto.ExecutionRequestMessage;
import com.solvemeup.smuworkerapi.worker.dto.ExecutionResultMessage;
import com.solvemeup.smuworkerapi.worker.dto.ExecutionTestCase;
import com.solvemeup.smuworkerapi.worker.enums.JudgeResult;
import com.solvemeup.smuworkerapi.worker.message.ExecutionResultProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

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
            String executableCode = codeGenerator.generateBatchExecutableCode(
                    request.language(),
                    request.sourceCode(),
                    request.functionName(),
                    request.parameters(),
                    request.returnType()
            );

            List<ExecutionTestCase> testCases = request.testCases();
            List<String> caseInputs = testCases.stream()
                    .map(testCase -> argumentConverter.toStdinInput(testCase.arguments(), request.parameters()))
                    .toList();

            DockerExecutor.BatchExecutionResult batch = dockerExecutor.executeBatch(
                    request.language(),
                    executableCode,
                    caseInputs,
                    request.timeLimitMillis(),
                    request.memoryLimitKilobytes()
            );

            for (int i = 0; i < testCases.size(); i++) {
                ExecutionTestCase testCase = testCases.get(i);
                DockerExecutor.BatchCaseResult caseResult = batch.cases().get(i);

                log.info("Run case {} executed - status: {}, time: {}ms, memory: {}KB",
                        testCase.caseIndex(), caseResult.status(),
                        caseResult.executionTimeMillis(), caseResult.memoryUsageKB());

                if (caseResult.status() == JudgeResult.CE) {
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

                JudgeResult status = caseResult.status();
                if (status == JudgeResult.AC) {
                    boolean isCorrect = outputComparator.compare(
                            caseResult.output(), testCase.expectedOutput(), request.returnType());
                    status = isCorrect ? JudgeResult.AC : JudgeResult.WA;
                }

                resultProducer.sendResult(new ExecutionResultMessage(
                        request.executionId(),
                        testCase.caseIndex(),
                        status,
                        testCase.arguments(),
                        testCase.expectedOutput(),
                        caseResult.output(),
                        caseResult.executionTimeMillis(),
                        caseResult.memoryUsageKB()
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
