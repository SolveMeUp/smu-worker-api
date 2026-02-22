package com.solvemeup.smuworkerapi.worker.dto;

import com.solvemeup.smuworkerapi.worker.enums.Language;
import com.solvemeup.smuworkerapi.worker.enums.ValueType;

import java.util.List;

public record RunRequestMessage(
        Long runId,
        String functionName,
        List<ParameterSpec> parameters,
        ValueType returnType,
        List<RunSampleCase> testCases,
        int timeLimitMillis,
        int memoryLimitKilobytes,
        Language language,
        String sourceCode
) {}
