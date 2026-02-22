package com.solvemeup.smuworkerapi.worker.service;

import com.solvemeup.smuworkerapi.worker.dto.ParameterSpec;
import com.solvemeup.smuworkerapi.worker.enums.ValueType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Slf4j
@Service
public class ArgumentConverter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * argumentsJson (JSON array of arg strings) + parameter specs → stdin string
     * e.g. argumentsJson="[\"[2,7,11,15]\",\"9\"]", params=[INT_ARRAY, INT]
     *      → "2 7 11 15\n9"
     */
    public String toStdinInput(String argumentsJson, List<ParameterSpec> params) {
        try {
            JsonNode argsNode = objectMapper.readTree(argumentsJson);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < params.size(); i++) {
                String arg = argsNode.get(i).asText();
                ValueType type = params.get(i).type();
                String stdinLine = convertArgToStdinLines(arg, type);
                if (i > 0) sb.append("\n");
                sb.append(stdinLine);
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert argumentsJson to stdin", e);
        }
    }

    private String convertArgToStdinLines(String arg, ValueType type) {
        try {
            return switch (type) {
                case INT, LONG, BOOLEAN -> arg;
                case STRING -> arg;
                case INT_ARRAY, LONG_ARRAY, BOOLEAN_ARRAY -> {
                    JsonNode node = objectMapper.readTree(arg);
                    List<String> elements = new ArrayList<>();
                    for (JsonNode element : node) {
                        elements.add(element.asText());
                    }
                    yield String.join(" ", elements);
                }
                case STRING_ARRAY -> {
                    JsonNode node = objectMapper.readTree(arg);
                    StringBuilder sb = new StringBuilder();
                    sb.append(node.size()).append("\n");
                    for (int i = 0; i < node.size(); i++) {
                        if (i > 0) sb.append("\n");
                        sb.append(node.get(i).asText());
                    }
                    yield sb.toString();
                }
                case INT_2D_ARRAY, LONG_2D_ARRAY, BOOLEAN_2D_ARRAY, STRING_2D_ARRAY -> {
                    JsonNode node = objectMapper.readTree(arg);
                    int rows = node.size();
                    int cols = rows > 0 ? node.get(0).size() : 0;
                    StringBuilder sb = new StringBuilder();
                    sb.append(rows).append(" ").append(cols);
                    for (int i = 0; i < rows; i++) {
                        sb.append("\n");
                        JsonNode row = node.get(i);
                        for (int j = 0; j < row.size(); j++) {
                            if (j > 0) sb.append(" ");
                            sb.append(row.get(j).asText());
                        }
                    }
                    yield sb.toString();
                }
            };
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert arg for type " + type, e);
        }
    }

    /**
     * stdin string + parameter specs → list of argument strings (for reporting)
     * e.g. stdin="2 7 11 15\n9", params=[INT_ARRAY, INT]
     *      → ["[2,7,11,15]", "9"]
     */
    public List<String> stdinToArguments(String stdin, List<ParameterSpec> params) {
        Scanner scanner = new Scanner(stdin);
        List<String> result = new ArrayList<>();
        for (ParameterSpec param : params) {
            result.add(extractArgFromStdin(scanner, param.type()));
        }
        return result;
    }

    private String extractArgFromStdin(Scanner scanner, ValueType type) {
        return switch (type) {
            case INT, LONG, BOOLEAN -> {
                String line = scanner.nextLine().trim();
                yield line.split("\\s+")[0];
            }
            case STRING -> scanner.nextLine().trim();
            case INT_ARRAY, LONG_ARRAY, BOOLEAN_ARRAY -> {
                String line = scanner.nextLine().trim();
                String[] elements = line.split("\\s+");
                yield "[" + String.join(",", elements) + "]";
            }
            case STRING_ARRAY -> {
                int count = Integer.parseInt(scanner.nextLine().trim());
                List<String> items = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    items.add("\"" + scanner.nextLine().trim() + "\"");
                }
                yield "[" + String.join(",", items) + "]";
            }
            case INT_2D_ARRAY, LONG_2D_ARRAY, BOOLEAN_2D_ARRAY -> {
                String firstLine = scanner.nextLine().trim();
                String[] dims = firstLine.split("\\s+");
                int rows = Integer.parseInt(dims[0]);
                List<String> rowStrings = new ArrayList<>();
                for (int i = 0; i < rows; i++) {
                    String rowLine = scanner.nextLine().trim();
                    String[] elements = rowLine.split("\\s+");
                    rowStrings.add("[" + String.join(",", elements) + "]");
                }
                yield "[" + String.join(",", rowStrings) + "]";
            }
            case STRING_2D_ARRAY -> {
                String firstLine = scanner.nextLine().trim();
                String[] dims = firstLine.split("\\s+");
                int rows = Integer.parseInt(dims[0]);
                List<String> rowStrings = new ArrayList<>();
                for (int i = 0; i < rows; i++) {
                    String rowLine = scanner.nextLine().trim();
                    String[] elements = rowLine.split("\\s+");
                    List<String> quoted = new ArrayList<>();
                    for (String e : elements) {
                        quoted.add("\"" + e + "\"");
                    }
                    rowStrings.add("[" + String.join(",", quoted) + "]");
                }
                yield "[" + String.join(",", rowStrings) + "]";
            }
        };
    }
}
