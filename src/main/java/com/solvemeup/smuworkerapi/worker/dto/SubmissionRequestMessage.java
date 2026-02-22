package com.solvemeup.smuworkerapi.worker.dto;

import com.solvemeup.smuworkerapi.worker.enums.Language;
import com.solvemeup.smuworkerapi.worker.enums.ValueType;

import java.util.List;

public record SubmissionRequestMessage(
        Long submissionResultId,
        Long submissionId,
        Long problemId,
        String functionName,
        List<ParameterSpec> parameters,
        ValueType returnType,
        int timeLimitMillis,
        int memoryLimitKilobytes,
        Language language,
        String sourceCode
) {}
