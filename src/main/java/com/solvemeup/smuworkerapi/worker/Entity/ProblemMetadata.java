package com.solvemeup.smuworkerapi.worker.Entity;

import java.util.List;

public record ProblemMetadata(
        String methodName,
        String returnType,
        List<Parameter> parameters
) {
    public record Parameter(
            String name,
            String type
    ){}
}
