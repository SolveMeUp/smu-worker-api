package com.solvemeup.smuworkerapi.worker.dto;

import com.solvemeup.smuworkerapi.worker.enums.Language;

public record JudgeRequestMessage(
        Long judgeId,
        Long submissionId,
        Long problemId,
        int timeLimitMillis,
        int memoryLimitMegabytes,
        Language language,
        String sourceCode
) {}