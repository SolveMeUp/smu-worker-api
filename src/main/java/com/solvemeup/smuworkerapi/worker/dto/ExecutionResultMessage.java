package com.solvemeup.smuworkerapi.worker.dto;

import com.solvemeup.smuworkerapi.worker.enums.JudgeResult;

public record ExecutionResultMessage(
        Long executionId,
        int caseIndex,
        JudgeResult verdict,
        String arguments,
        String expectedOutput,
        String actualOutput,
        Integer timeUsedMillis,
        Integer memoryUsedKilobytes
) {}
