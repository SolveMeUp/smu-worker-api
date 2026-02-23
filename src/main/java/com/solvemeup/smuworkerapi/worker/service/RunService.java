package com.solvemeup.smuworkerapi.worker.service;

import com.solvemeup.smuworkerapi.worker.Entity.TestCase;
import com.solvemeup.smuworkerapi.worker.dto.RunRequestMessage;
import com.solvemeup.smuworkerapi.worker.dto.RunResultMessage;
import com.solvemeup.smuworkerapi.worker.dto.RunSampleCase;
import com.solvemeup.smuworkerapi.worker.enums.JudgeResult;
import com.solvemeup.smuworkerapi.worker.message.RunResultProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunService {

    private final TestCaseLoader testCaseLoader;
    private final CodeGenerator codeGenerator;
    private final DockerExecutor dockerExecutor;
    private final OutputComparator outputComparator;
    private final ArgumentConverter argumentConverter;
    private final RunResultProducer runResultProducer;

    public void judge(RunRequestMessage request) {
        log.info("Starting run - runId: {}, language: {}, cases: {}",
                request.runId(), request.language(), request.testCases().size());

        try {
            List<TestCase> testCases = testCaseLoader.loadTestCases(request.runId());

            String executableCode = codeGenerator.generateExecutableCode(
                    request.language(),
                    request.sourceCode(),
                    request.functionName(),
                    request.parameters(),
                    request.returnType()
            );

            boolean ceDetected = false;

            for (RunSampleCase sampleCase : request.testCases()) {
                if (ceDetected) {
                    runResultProducer.sendResult(new RunResultMessage(
                            request.runId(),
                            sampleCase.caseIndex(),
                            JudgeResult.CE,
                            null, null, null, null
                    ));
                    continue;
                }

                String stdin = argumentConverter.toStdinInput(
                        sampleCase.argumentsJson(), request.parameters());

                String expectedOutput = getExpectedOutput(testCases, sampleCase.caseIndex());

                DockerExecutor.ExecutionResult execResult = dockerExecutor.execute(
                        request.language(),
                        executableCode,
                        stdin,
                        sampleCase.caseIndex() + 1,
                        request.timeLimitMillis(),
                        request.memoryLimitKilobytes()
                );

                log.info("Run case {} executed - status: {}, time: {}ms, memory: {}KB",
                        sampleCase.caseIndex(), execResult.status(),
                        execResult.executionTimeMillis(), execResult.memoryUsageKB());

                if (execResult.status() == JudgeResult.CE) {
                    ceDetected = true;
                    runResultProducer.sendResult(new RunResultMessage(
                            request.runId(),
                            sampleCase.caseIndex(),
                            JudgeResult.CE,
                            null, null, null, null
                    ));
                    continue;
                }

                JudgeResult status = execResult.status();
                if (status == JudgeResult.AC) {
                    boolean isCorrect = outputComparator.compare(execResult.output(), expectedOutput);
                    status = isCorrect ? JudgeResult.AC : JudgeResult.WA;
                }

                runResultProducer.sendResult(new RunResultMessage(
                        request.runId(),
                        sampleCase.caseIndex(),
                        status,
                        expectedOutput,
                        execResult.output(),
                        execResult.executionTimeMillis(),
                        execResult.memoryUsageKB()
                ));
            }

            log.info("Run completed - runId: {}", request.runId());

        } catch (Exception e) {
            log.error("Run failed with exception - runId: {}", request.runId(), e);
            for (RunSampleCase sampleCase : request.testCases()) {
                runResultProducer.sendResult(new RunResultMessage(
                        request.runId(),
                        sampleCase.caseIndex(),
                        JudgeResult.SYSTEM_ERROR,
                        null, null, null, null
                ));
            }
        }
    }

    private String getExpectedOutput(List<TestCase> testCases, int caseIndex) {
        if (caseIndex < 0 || caseIndex >= testCases.size()) {
            log.warn("caseIndex {} out of range (testCases size: {})", caseIndex, testCases.size());
            return null;
        }
        return testCases.get(caseIndex).expectedOutput();
    }
}
