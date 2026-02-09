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

    private String generateJavaCode(String userCode, ProblemMetadata metadata) {
        StringBuilder code = new StringBuilder();

        code.append("import java.util.*;\n");
        code.append("import java.io.*;\n\n");

        code.append(userCode).append("\n\n");

        code.append("public class Main {\n");
        code.append("    public static void main(String[] args) {\n");
        code.append("        Scanner sc = new Scanner(System.in);\n");
        code.append("        Solution solution = new Solution();\n\n");

        List<ProblemMetadata.Parameter> params = metadata.parameters();
        for (ProblemMetadata.Parameter param : params) {
            code.append(generateJavaParameterParsing(param));
        }

        code.append("\n        ");
        if (!"void".equals(metadata.returnType())) {
            code.append(metadata.returnType()).append(" result = ");
        }
        code.append("solution.").append(metadata.methodName()).append("(");
        code.append(params.stream()
                .map(ProblemMetadata.Parameter::name)
                .collect(Collectors.joining(", ")));
        code.append(");\n\n");

        if (!"void".equals(metadata.returnType())) {
            code.append(generateJavaOutputCode(metadata.returnType()));
        }

        code.append("        sc.close();\n");
        code.append("    }\n");
        code.append("}\n");

        log.debug("Generated Java code:\n{}", code);
        return code.toString();
    }

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


    private String generateJavaOutputCode(String returnType) {
        StringBuilder code = new StringBuilder();

        if (returnType.equals("int") || returnType.equals("long") ||
                returnType.equals("double") || returnType.equals("String")) {
            code.append("        System.out.println(result);\n");
            return code.toString();
        }

        if (returnType.equals("int[]")) {
            code.append("        for (int i = 0; i < result.length; i++) {\n");
            code.append("            if (i > 0) System.out.print(\" \");\n");
            code.append("            System.out.print(result[i]);\n");
            code.append("        }\n");
            code.append("        System.out.println();\n");
            return code.toString();
        }

        if (returnType.equals("long[]")) {
            code.append("        for (int i = 0; i < result.length; i++) {\n");
            code.append("            if (i > 0) System.out.print(\" \");\n");
            code.append("            System.out.print(result[i]);\n");
            code.append("        }\n");
            code.append("        System.out.println();\n");
            return code.toString();
        }

        if (returnType.equals("String[]")) {
            code.append("        System.out.println(result.length);\n");
            code.append("        for (String val : result) {\n");
            code.append("            System.out.println(val);\n");
            code.append("        }\n");
            return code.toString();
        }

        if (returnType.equals("int[][]")) {
            code.append("        System.out.println(result.length + \" \" + result[0].length);\n");
            code.append("        for (int[] row : result) {\n");
            code.append("            for (int val : row) {\n");
            code.append("                System.out.print(val + \" \");\n");
            code.append("            }\n");
            code.append("            System.out.println();\n");
            code.append("        }\n");
            return code.toString();
        }

        throw new IllegalArgumentException("Unsupported return type: " + returnType);
    }

    private String generatePythonCode(String userCode, ProblemMetadata metadata) {
        StringBuilder code = new StringBuilder();

        code.append(userCode).append("\n\n");

        code.append("if __name__ == '__main__':\n");
        code.append("    import sys\n");
        code.append("    input_lines = sys.stdin.read().strip().split('\\n')\n");
        code.append("    line_idx = 0\n\n");

        List<ProblemMetadata.Parameter> params = metadata.parameters();

        for (ProblemMetadata.Parameter param : params) {
            code.append(generatePythonParameterParsing(param));
        }

        code.append("\n    solution = Solution()\n");
        code.append("    result = solution.").append(metadata.methodName()).append("(");
        code.append(params.stream()
                .map(ProblemMetadata.Parameter::name)
                .collect(Collectors.joining(", ")));
        code.append(")\n\n");

        if (!"void".equals(metadata.returnType())) {
            code.append(generatePythonOutputCode(metadata.returnType()));
        }

        log.debug("Generated Python code:\n{}", code);
        return code.toString();
    }

    private String generatePythonParameterParsing(ProblemMetadata.Parameter param) {
        String type = param.type();
        String name = param.name();

        if (type.equals("int")) {
            return "    " + name + " = int(input_lines[line_idx].split()[0])\n" +
                    "    line_idx += 1\n";
        }
        if (type.equals("String")) {
            return "    " + name + " = input_lines[line_idx]\n" +
                    "    line_idx += 1\n";
        }
        if (type.equals("int[]")) {
            return "    " + name + " = list(map(int, input_lines[line_idx].split()))\n" +
                    "    line_idx += 1\n";
        }

        if (type.equals("int[][]")) {
            return "    tokens = input_lines[line_idx].split()\n" +
                    "    rows = int(tokens[0])\n" +
                    "    cols = int(tokens[1])\n" +
                    "    line_idx += 1\n" +
                    "    " + name + " = []\n" +
                    "    for i in range(rows):\n" +
                    "        row = list(map(int, input_lines[line_idx].split()))\n" +
                    "        " + name + ".append(row)\n" +
                    "        line_idx += 1\n";
        }

        throw new IllegalArgumentException("Unsupported parameter type: " + type);
    }

    private String generatePythonOutputCode(String returnType) {
        if (returnType.equals("int") || returnType.equals("String")) {
            return "    print(result)\n";
        }
        if (returnType.equals("int[]")) {
            return "    print(' '.join(map(str, result)))\n";
        }

        if (returnType.equals("int[][]")) {
            return "    print(len(result), len(result[0]))\n" +
                    "    for row in result:\n" +
                    "        print(' '.join(map(str, row)))\n";
        }

        throw new IllegalArgumentException("Unsupported return type: " + returnType);
    }

    private String generateCppParameterParsing(ProblemMetadata.Parameter param) {
        String type = param.type();
        String name = param.name();

        if (type.equals("int")) {
            return "    int " + name + ";\n" +
                    "    cin >> " + name + ";\n";
        }
        if (type.equals("String")) {
            return "    string " + name + ";\n" +
                    "    getline(cin, " + name + ");\n";
        }
        if (type.equals("int[]")) {
            return "    string line_" + name + ";\n" +
                    "    getline(cin, line_" + name + ");\n" +
                    "    istringstream iss_" + name + "(line_" + name + ");\n" +
                    "    vector<int> " + name + ";\n" +
                    "    { int val; while (iss_" + name + " >> val) " + name + ".push_back(val); }\n";
        }

        if (type.equals("int[][]")) {
            return "    int rows_" + name + ", cols_" + name + ";\n" +
                    "    cin >> rows_" + name + " >> cols_" + name + ";\n" +
                    "    vector<vector<int>> " + name + "(rows_" + name + ", vector<int>(cols_" + name + "));\n" +
                    "    for (int i = 0; i < rows_" + name + "; i++) {\n" +
                    "        for (int j = 0; j < cols_" + name + "; j++) {\n" +
                    "            cin >> " + name + "[i][j];\n" +
                    "        }\n" +
                    "    }\n";
        }

        throw new IllegalArgumentException("Unsupported parameter type: " + type);
    }


    private String generateCppOutputCode(String returnType) {
        if (returnType.equals("int")) {
            return "    cout << result << endl;\n";
        }
        if (returnType.equals("String")) {
            return "    cout << result << endl;\n";
        }
        if (returnType.equals("int[]")) {
            return "    for (int i = 0; i < result.size(); i++) {\n" +
                    "        if (i > 0) cout << \" \";\n" +
                    "        cout << result[i];\n" +
                    "    }\n" +
                    "    cout << endl;\n";
        }

        if (returnType.equals("int[][]")) {
            return "    cout << result.size() << \" \" << result[0].size() << endl;\n" +
                    "    for (auto& row : result) {\n" +
                    "        for (int val : row) {\n" +
                    "            cout << val << \" \";\n" +
                    "        }\n" +
                    "        cout << endl;\n" +
                    "    }\n";
        }

        throw new IllegalArgumentException("Unsupported return type: " + returnType);
    }

}