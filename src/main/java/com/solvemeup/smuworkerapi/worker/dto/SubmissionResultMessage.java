package com.solvemeup.smuworkerapi.worker.dto;

import com.solvemeup.smuworkerapi.worker.enums.JudgeResult;

public record SubmissionResultMessage(
        Long submissionId,
        JudgeResult verdict,
        Integer failedCaseIndex,
        String arguments,
        String expectedOutput,
        String actualOutput,
        Integer timeUsedMillis,
        Integer memoryUsedKilobytes
) {}
