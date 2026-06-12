package com.solvemeup.smuworkerapi.worker.dto;

import com.solvemeup.smuworkerapi.worker.enums.Language;
import com.solvemeup.smuworkerapi.worker.enums.ValueType;

import java.util.List;

public record ExecutionRequestMessage(
        Long executionId,
        Long problemId,
        String functionName,
        List<ParameterSpec> parameters,
        ValueType returnType,
        List<ExecutionTestCase> testCases,
        int timeLimitMillis,
        int memoryLimitKilobytes,
        Language language,
        String sourceCode
) {}
