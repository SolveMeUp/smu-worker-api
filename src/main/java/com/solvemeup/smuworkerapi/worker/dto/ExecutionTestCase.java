package com.solvemeup.smuworkerapi.worker.dto;

public record ExecutionTestCase(
        int caseIndex,
        String arguments,
        String expectedOutput
) {}
