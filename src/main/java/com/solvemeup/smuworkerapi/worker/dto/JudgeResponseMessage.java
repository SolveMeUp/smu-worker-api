package com.solvemeup.smuworkerapi.worker.dto;

import com.solvemeup.smuworkerapi.worker.enums.JudgeResult;

public record JudgeResponseMessage(
        Long judgeId,
        Long submissionId,
        JudgeResult result,
        Integer timeUsedMillis,
        Integer memoryUsedMegabytes
) {}
