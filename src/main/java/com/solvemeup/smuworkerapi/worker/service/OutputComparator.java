package com.solvemeup.smuworkerapi.worker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OutputComparator {

    public boolean compare(String actual, String expected) {
        if (actual == null || expected == null) {
            log.warn("Null output detected - actual: {}, expected: {}",
                    actual != null, expected != null);
            return false;
        }

        String normalizedActual = normalize(actual);
        String normalizedExpected = normalize(expected);

        boolean isEqual = normalizedActual.equals(normalizedExpected);

        if (!isEqual) {
            log.debug("Output mismatch:\nExpected: [{}]\nActual: [{}]",
                    normalizedExpected, normalizedActual);
        }

        return isEqual;
    }

    private String normalize(String output) {
        return output
                .replaceAll("\r\n", "\n")
                .replaceAll("\r", "\n")
                .trim()
                .replaceAll("(?m)\\s+$", "")
                .replaceAll(" +", " ");
    }
}