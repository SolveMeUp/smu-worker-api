package com.solvemeup.smuworkerapi.worker.dto;

public record RunSampleCase(
        int caseIndex,
        String argumentsJson
) {}
