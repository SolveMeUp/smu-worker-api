package com.solvemeup.smuworkerapi.worker.service;

import com.solvemeup.smuworkerapi.worker.enums.ValueType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class OutputComparator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean compare(String actual, String expected) {
        return compare(actual, expected, null);
    }

    public boolean compare(String actual, String expected, ValueType returnType) {
        if (actual == null || expected == null) {
            log.warn("Null output detected - actual: {}, expected: {}",
                    actual != null, expected != null);
            return false;
        }

        Boolean jsonComparison = compareJson(actual, expected);
        if (jsonComparison != null) {
            return jsonComparison;
        }

        boolean preserveSpaces = returnType != null && isTextType(returnType);
        String normalizedActual = normalize(actual, preserveSpaces);
        String normalizedExpected = normalize(expected, preserveSpaces);
        boolean isEqual = normalizedActual.equals(normalizedExpected);

        if (!isEqual && returnType != null) {
            String legacyExpected = jsonToLegacyOutput(expected, returnType);
            isEqual = legacyExpected != null
                    && normalizedActual.equals(normalize(legacyExpected, preserveSpaces));
        }

        if (!isEqual) {
            log.debug("Output mismatch:\nExpected: [{}]\nActual: [{}]",
                    expected, actual);
        }

        return isEqual;
    }

    private String jsonToLegacyOutput(String output, ValueType returnType) {
        try {
            JsonNode node = objectMapper.readTree(output.trim());
            int dimensions = dimensions(returnType);
            if (dimensions == 0) {
                return node.isTextual() ? node.asText() : node.toString();
            }
            if (returnType == ValueType.STRING_ARRAY) {
                StringBuilder result = new StringBuilder().append(node.size());
                node.forEach(value -> result.append('\n').append(value.asText()));
                return result.toString();
            }
            if (dimensions == 1) {
                return join(node);
            }
            if (dimensions == 2) {
                int rows = node.size();
                int columns = rows == 0 ? 0 : node.get(0).size();
                StringBuilder result = new StringBuilder(rows + " " + columns);
                node.forEach(row -> result.append('\n').append(join(row)));
                return result.toString();
            }

            int depth = node.size();
            int rows = depth == 0 ? 0 : node.get(0).size();
            int columns = rows == 0 ? 0 : node.get(0).get(0).size();
            StringBuilder result = new StringBuilder(depth + " " + rows + " " + columns);
            node.forEach(matrix -> matrix.forEach(row -> result.append('\n').append(join(row))));
            return result.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String join(JsonNode array) {
        StringBuilder result = new StringBuilder();
        for (JsonNode value : array) {
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(value.isTextual() ? value.asText() : value.toString());
        }
        return result.toString();
    }

    private int dimensions(ValueType type) {
        String name = type.name();
        if (name.endsWith("_3D_ARRAY")) {
            return 3;
        }
        if (name.endsWith("_2D_ARRAY")) {
            return 2;
        }
        return name.endsWith("_ARRAY") ? 1 : 0;
    }

    private boolean isTextType(ValueType type) {
        return type.name().startsWith("STRING") || type.name().startsWith("CHAR");
    }

    private Boolean compareJson(String actual, String expected) {
        try {
            JsonNode actualNode = objectMapper.readTree(actual.trim());
            JsonNode expectedNode = objectMapper.readTree(expected.trim());
            return actualNode.equals(expectedNode);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String normalize(String output, boolean preserveSpaces) {
        String normalized = output
                .replaceAll("\r\n", "\n")
                .replaceAll("\r", "\n")
                .trim()
                .replaceAll("(?m)[\\t ]+$", "");
        return preserveSpaces ? normalized : normalized.replaceAll(" +", " ");
    }
}
