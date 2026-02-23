package com.solvemeup.smuworkerapi.worker.dto;

import com.solvemeup.smuworkerapi.worker.enums.JudgeResult;

import java.util.List;

public record SubmissionResultMessage(
        Long submissionResultId,
        Long submissionId,
        Long problemId,
        JudgeResult result,
        Integer failedTestIndex,
        String expectedOutput,
        String actualOutput,
        List<String> arguments,
        Integer timeUsedMillis,
        Integer memoryUsedKilobytes
) {}
