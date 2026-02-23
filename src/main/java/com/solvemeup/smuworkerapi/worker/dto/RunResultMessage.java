package com.solvemeup.smuworkerapi.worker.dto;

import com.solvemeup.smuworkerapi.worker.enums.JudgeResult;

public record RunResultMessage(
        Long executionId,
        int caseIndex,
        JudgeResult status,
        String expectedOutput,
        String actualOutput,
        Integer timeUsedMillis,
        Integer memoryUsedKilobytes
) {}
