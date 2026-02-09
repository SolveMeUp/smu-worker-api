package com.solvemeup.smuworkerapi.worker.service;

import com.solvemeup.smuworkerapi.worker.Entity.ProblemMetadata;
import com.solvemeup.smuworkerapi.worker.enums.Language;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CodeGenerator {

    private String generateJavaParameterParsing(ProblemMetadata.Parameter param) {
        String type = param.type();
        String name = param.name();

        if (type.equals("int")) {
            return "        int " + name + " = sc.nextInt();\n";
        }
        if (type.equals("long")) {
            return "        long " + name + " = sc.nextLong();\n";
        }
        if (type.equals("double")) {
            return "        double " + name + " = sc.nextDouble();\n";
        }
        if (type.equals("String")) {
            return "        String " + name + " = sc.nextLine().trim();\n";
        }

        if (type.equals("int[]")) {
            return "        String line_" + name + " = sc.nextLine().trim();\n" +
                    "        if (line_" + name + ".isEmpty() && sc.hasNextLine()) line_" + name + " = sc.nextLine().trim();\n" +
                    "        String[] tokens_" + name + " = line_" + name + ".split(\"\\\\s+\");\n" +
                    "        int[] " + name + " = new int[tokens_" + name + ".length];\n" +
                    "        for (int i = 0; i < tokens_" + name + ".length; i++) {\n" +
                    "            " + name + "[i] = Integer.parseInt(tokens_" + name + "[i]);\n" +
                    "        }\n";
        }

        if (type.equals("long[]")) {
            return "        String line_" + name + " = sc.nextLine().trim();\n" +
                    "        if (line_" + name + ".isEmpty() && sc.hasNextLine()) line_" + name + " = sc.nextLine().trim();\n" +
                    "        String[] tokens_" + name + " = line_" + name + ".split(\"\\\\s+\");\n" +
                    "        long[] " + name + " = new long[tokens_" + name + ".length];\n" +
                    "        for (int i = 0; i < tokens_" + name + ".length; i++) {\n" +
                    "            " + name + "[i] = Long.parseLong(tokens_" + name + "[i]);\n" +
                    "        }\n";
        }

        if (type.equals("String[]")) {
            return "        int size_" + name + " = Integer.parseInt(sc.nextLine().trim());\n" +
                    "        String[] " + name + " = new String[size_" + name + "];\n" +
                    "        for (int i = 0; i < size_" + name + "; i++) {\n" +
                    "            " + name + "[i] = sc.nextLine().trim();\n" +
                    "        }\n";
        }

        if (type.equals("int[][]")) {
            return "        int rows_" + name + " = sc.nextInt();\n" +
                    "        int cols_" + name + " = sc.nextInt();\n" +
                    "        int[][] " + name + " = new int[rows_" + name + "][cols_" + name + "];\n" +
                    "        for (int i = 0; i < rows_" + name + "; i++) {\n" +
                    "            for (int j = 0; j < cols_" + name + "; j++) {\n" +
                    "                " + name + "[i][j] = sc.nextInt();\n" +
                    "            }\n" +
                    "        }\n";
        }

        throw new IllegalArgumentException("Unsupported parameter type: " + type);
    }
}