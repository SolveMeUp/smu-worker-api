package com.solvemeup.smuworkerapi.worker.service;

import com.solvemeup.smuworkerapi.worker.dto.ParameterSpec;
import com.solvemeup.smuworkerapi.worker.enums.ValueType;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Service
public class ArgumentConverter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String toStdinInput(String arguments, List<ParameterSpec> parameters) {
        try {
            JsonNode argumentsNode = objectMapper.readTree(arguments);
            if (!argumentsNode.isArray() || argumentsNode.size() != parameters.size()) {
                throw new IllegalArgumentException("arguments must match the parameter count");
            }

            List<String> blocks = new ArrayList<>();
            for (int i = 0; i < parameters.size(); i++) {
                blocks.add(toStdinBlock(argumentsNode.get(i), parameters.get(i).type()));
            }
            return String.join("\n", blocks);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert arguments to stdin", e);
        }
    }

    public String stdinToArgumentsJson(String stdin, List<ParameterSpec> parameters) {
        if (stdin == null) {
            return null;
        }

        Scanner scanner = new Scanner(stdin);
        List<String> arguments = new ArrayList<>();
        for (ParameterSpec parameter : parameters) {
            arguments.add(readJsonArgument(scanner, parameter.type()));
        }
        return "[" + String.join(",", arguments) + "]";
    }

    List<String> stdinToArguments(String stdin, List<ParameterSpec> parameters) {
        try {
            JsonNode arguments = objectMapper.readTree(stdinToArgumentsJson(stdin, parameters));
            List<String> values = new ArrayList<>();
            arguments.forEach(value -> values.add(value.isTextual() ? value.asText() : value.toString()));
            return values;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert stdin to arguments", e);
        }
    }

    private String toStdinBlock(JsonNode node, ValueType type) {
        if (node.isTextual() && TypeShape.dimensions(type) > 0) {
            try {
                node = objectMapper.readTree(node.asText());
            } catch (Exception e) {
                throw new IllegalArgumentException("Array argument must contain valid JSON", e);
            }
        }
        return switch (type) {
            case INT, LONG, BOOLEAN, DOUBLE -> node.asText();
            case CHAR, STRING -> node.asText();
            case INT_ARRAY, LONG_ARRAY, BOOLEAN_ARRAY, CHAR_ARRAY, DOUBLE_ARRAY ->
                    joinArray(node);
            case STRING_ARRAY -> stringArrayToStdin(node);
            case INT_2D_ARRAY, LONG_2D_ARRAY, BOOLEAN_2D_ARRAY, CHAR_2D_ARRAY,
                    DOUBLE_2D_ARRAY, STRING_2D_ARRAY -> twoDimensionalArrayToStdin(node);
            case INT_3D_ARRAY, LONG_3D_ARRAY, BOOLEAN_3D_ARRAY, CHAR_3D_ARRAY,
                    DOUBLE_3D_ARRAY, STRING_3D_ARRAY -> threeDimensionalArrayToStdin(node);
        };
    }

    private String joinArray(JsonNode node) {
        List<String> values = new ArrayList<>();
        node.forEach(value -> values.add(value.asText()));
        return String.join(" ", values);
    }

    private String stringArrayToStdin(JsonNode node) {
        List<String> lines = new ArrayList<>();
        lines.add(Integer.toString(node.size()));
        node.forEach(value -> lines.add(value.asText()));
        return String.join("\n", lines);
    }

    private String twoDimensionalArrayToStdin(JsonNode node) {
        int rows = node.size();
        int columns = rows == 0 ? 0 : node.get(0).size();
        List<String> lines = new ArrayList<>();
        lines.add(rows + " " + columns);
        node.forEach(row -> lines.add(joinArray(row)));
        return String.join("\n", lines);
    }

    private String threeDimensionalArrayToStdin(JsonNode node) {
        int depth = node.size();
        int rows = depth == 0 ? 0 : node.get(0).size();
        int columns = rows == 0 ? 0 : node.get(0).get(0).size();
        List<String> lines = new ArrayList<>();
        lines.add(depth + " " + rows + " " + columns);
        node.forEach(matrix -> matrix.forEach(row -> lines.add(joinArray(row))));
        return String.join("\n", lines);
    }

    private String readJsonArgument(Scanner scanner, ValueType type) {
        return switch (type) {
            case INT, LONG, BOOLEAN, DOUBLE -> nextLine(scanner);
            case CHAR, STRING -> quote(nextLine(scanner));
            case INT_ARRAY, LONG_ARRAY, BOOLEAN_ARRAY, DOUBLE_ARRAY ->
                    "[" + commaSeparated(nextLine(scanner)) + "]";
            case CHAR_ARRAY -> quoteArray(nextLine(scanner));
            case STRING_ARRAY -> readStringArray(scanner);
            case INT_2D_ARRAY, LONG_2D_ARRAY, BOOLEAN_2D_ARRAY, DOUBLE_2D_ARRAY ->
                    readTwoDimensionalArray(scanner, false);
            case CHAR_2D_ARRAY, STRING_2D_ARRAY ->
                    readTwoDimensionalArray(scanner, true);
            case INT_3D_ARRAY, LONG_3D_ARRAY, BOOLEAN_3D_ARRAY, DOUBLE_3D_ARRAY ->
                    readThreeDimensionalArray(scanner, false);
            case CHAR_3D_ARRAY, STRING_3D_ARRAY ->
                    readThreeDimensionalArray(scanner, true);
        };
    }

    private String readStringArray(Scanner scanner) {
        int size = Integer.parseInt(nextLine(scanner));
        List<String> values = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            values.add(quote(nextLine(scanner)));
        }
        return "[" + String.join(",", values) + "]";
    }

    private String readTwoDimensionalArray(Scanner scanner, boolean quoteValues) {
        String[] dimensions = nextLine(scanner).split("\\s+");
        int rows = Integer.parseInt(dimensions[0]);
        List<String> values = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            values.add(arrayLine(nextLine(scanner), quoteValues));
        }
        return "[" + String.join(",", values) + "]";
    }

    private String readThreeDimensionalArray(Scanner scanner, boolean quoteValues) {
        String[] dimensions = nextLine(scanner).split("\\s+");
        int depth = Integer.parseInt(dimensions[0]);
        int rows = Integer.parseInt(dimensions[1]);
        List<String> matrices = new ArrayList<>();
        for (int d = 0; d < depth; d++) {
            List<String> matrix = new ArrayList<>();
            for (int r = 0; r < rows; r++) {
                matrix.add(arrayLine(nextLine(scanner), quoteValues));
            }
            matrices.add("[" + String.join(",", matrix) + "]");
        }
        return "[" + String.join(",", matrices) + "]";
    }

    private String arrayLine(String line, boolean quoteValues) {
        if (line.isBlank()) {
            return "[]";
        }
        String[] values = line.trim().split("\\s+");
        List<String> jsonValues = new ArrayList<>();
        for (String value : values) {
            jsonValues.add(quoteValues ? quote(value) : value);
        }
        return "[" + String.join(",", jsonValues) + "]";
    }

    private String quoteArray(String line) {
        return arrayLine(line, true);
    }

    private String commaSeparated(String line) {
        return line.isBlank() ? "" : String.join(",", line.trim().split("\\s+"));
    }

    private String nextLine(Scanner scanner) {
        if (!scanner.hasNextLine()) {
            throw new IllegalArgumentException("stdin does not contain enough values");
        }
        return scanner.nextLine().trim();
    }

    private String quote(String value) {
        return objectMapper.writeValueAsString(value);
    }

    private static final class TypeShape {
        private static int dimensions(ValueType type) {
            String name = type.name();
            if (name.endsWith("_3D_ARRAY")) {
                return 3;
            }
            if (name.endsWith("_2D_ARRAY")) {
                return 2;
            }
            return name.endsWith("_ARRAY") ? 1 : 0;
        }
    }
}
