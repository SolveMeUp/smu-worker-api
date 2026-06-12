package com.solvemeup.smuworkerapi.worker;

import com.solvemeup.smuworkerapi.worker.config.RabbitConfig;
import com.solvemeup.smuworkerapi.worker.dto.ExecutionRequestMessage;
import com.solvemeup.smuworkerapi.worker.dto.ExecutionResultMessage;
import com.solvemeup.smuworkerapi.worker.dto.ExecutionTestCase;
import com.solvemeup.smuworkerapi.worker.dto.ParameterSpec;
import com.solvemeup.smuworkerapi.worker.dto.SubmissionRequestMessage;
import com.solvemeup.smuworkerapi.worker.dto.SubmissionResultMessage;
import com.solvemeup.smuworkerapi.worker.enums.JudgeResult;
import com.solvemeup.smuworkerapi.worker.enums.Language;
import com.solvemeup.smuworkerapi.worker.enums.ValueType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CoreMessageContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void rabbitNamesMatchCoreContract() {
        assertThat(RabbitConfig.SUBMISSION_EXCHANGE).isEqualTo("submission.exchange");
        assertThat(RabbitConfig.SUBMISSION_REQUEST_QUEUE).isEqualTo("submission.request.queue");
        assertThat(RabbitConfig.SUBMISSION_REQUEST_ROUTING_KEY).isEqualTo("submission.request");
        assertThat(RabbitConfig.SUBMISSION_RESULT_ROUTING_KEY).isEqualTo("submission.result");
        assertThat(RabbitConfig.EXECUTION_EXCHANGE).isEqualTo("execution.exchange");
        assertThat(RabbitConfig.EXECUTION_REQUEST_QUEUE).isEqualTo("execution.request.queue");
        assertThat(RabbitConfig.EXECUTION_REQUEST_ROUTING_KEY).isEqualTo("execution.request");
        assertThat(RabbitConfig.EXECUTION_RESULT_ROUTING_KEY).isEqualTo("execution.result");
    }

    @Test
    void executionMessagesMatchCoreJsonFields() throws Exception {
        ExecutionRequestMessage request = new ExecutionRequestMessage(
                10L,
                20L,
                "solve",
                List.of(new ParameterSpec("values", ValueType.INT_ARRAY)),
                ValueType.INT,
                List.of(new ExecutionTestCase(0, "[[1,2,3]]", "6")),
                2_000,
                262_144,
                Language.JAVA,
                "class Solution {}"
        );
        ExecutionResultMessage result = new ExecutionResultMessage(
                10L, 0, JudgeResult.AC, "[[1,2,3]]", "6", "6", 12, 1024
        );

        assertThat(fieldNames(request)).containsExactlyInAnyOrder(
                "executionId", "problemId", "functionName", "parameters", "returnType",
                "testCases", "timeLimitMillis", "memoryLimitKilobytes", "language", "sourceCode"
        );
        assertThat(fieldNames(result)).containsExactlyInAnyOrder(
                "executionId", "caseIndex", "verdict", "arguments", "expectedOutput",
                "actualOutput", "timeUsedMillis", "memoryUsedKilobytes"
        );
    }

    @Test
    void submissionMessagesMatchCoreJsonFields() throws Exception {
        SubmissionRequestMessage request = new SubmissionRequestMessage(
                30L,
                40L,
                "solve",
                List.of(new ParameterSpec("value", ValueType.INT)),
                ValueType.INT,
                2_000,
                262_144,
                Language.PYTHON,
                "class Solution: pass"
        );
        SubmissionResultMessage result = new SubmissionResultMessage(
                30L, JudgeResult.WA, 1, "[3]", "4", "5", 10, 512
        );

        assertThat(fieldNames(request)).containsExactlyInAnyOrder(
                "submissionId", "problemId", "functionName", "parameters", "returnType",
                "timeLimitMillis", "memoryLimitKilobytes", "language", "sourceCode"
        );
        assertThat(fieldNames(result)).containsExactlyInAnyOrder(
                "submissionId", "verdict", "failedCaseIndex", "arguments",
                "expectedOutput", "actualOutput", "timeUsedMillis", "memoryUsedKilobytes"
        );
    }

    private Set<String> fieldNames(Object message) throws Exception {
        JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(message));
        Set<String> names = new java.util.HashSet<>();
        names.addAll(node.propertyNames());
        return names;
    }
}
