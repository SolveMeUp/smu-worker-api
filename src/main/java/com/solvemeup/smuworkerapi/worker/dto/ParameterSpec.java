package com.solvemeup.smuworkerapi.worker.dto;

import com.solvemeup.smuworkerapi.worker.enums.ValueType;

public record ParameterSpec(
        String name,
        ValueType type
) {}
