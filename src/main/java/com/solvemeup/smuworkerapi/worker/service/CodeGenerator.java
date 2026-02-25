package com.solvemeup.smuworkerapi.worker.service;

import com.solvemeup.smuworkerapi.worker.dto.ParameterSpec;
import com.solvemeup.smuworkerapi.worker.enums.Language;
import com.solvemeup.smuworkerapi.worker.enums.ValueType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CodeGenerator {

    public String generateExecutableCode(
            Language language,
            String userCode,
            String functionName,
            List<ParameterSpec> parameters,
            ValueType returnType
    ) {
        return switch (language) {
            case JAVA -> generateJavaCode(userCode, functionName, parameters, returnType);
            case PYTHON -> generatePythonCode(userCode, functionName, parameters, returnType);
            case CPP -> generateCppCode(userCode, functionName, parameters, returnType);
        };
    }

    // ── Java ──────────────────────────────────────────────────────────────────

    private String generateJavaCode(
            String userCode, String functionName,
            List<ParameterSpec> params, ValueType returnType
    ) {
        StringBuilder code = new StringBuilder();

        code.append("import java.util.*;\n");
        code.append("import java.io.*;\n\n");

        code.append(userCode).append("\n\n");

        code.append("public class Main {\n");
        code.append("    public static void main(String[] args) throws Exception {\n");
        code.append("        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));\n");
        code.append("        Solution solution = new Solution();\n\n");

        for (ParameterSpec param : params) {
            code.append(generateJavaParameterParsing(param));
        }

        code.append("\n        ");
        code.append(valueTypeToJavaType(returnType)).append(" result = ");
        code.append("solution.").append(functionName).append("(");
        code.append(params.stream().map(ParameterSpec::name).collect(Collectors.joining(", ")));
        code.append(");\n\n");

        code.append(generateJavaOutputCode(returnType));

        code.append("    }\n");
        code.append("}\n");

        log.debug("Generated Java code:\n{}", code);
        return code.toString();
    }

    private String generateJavaParameterParsing(ParameterSpec param) {
        String name = param.name();
        return switch (param.type()) {
            case INT ->
                "        int " + name + " = Integer.parseInt(br.readLine().trim());\n";
            case LONG ->
                "        long " + name + " = Long.parseLong(br.readLine().trim());\n";
            case BOOLEAN ->
                "        boolean " + name + " = Boolean.parseBoolean(br.readLine().trim());\n";
            case STRING ->
                "        String " + name + " = br.readLine().trim();\n";
            case INT_ARRAY ->
                "        String[] tokens_" + name + " = br.readLine().trim().split(\"\\\\s+\");\n" +
                "        int[] " + name + " = new int[tokens_" + name + ".length];\n" +
                "        for (int i = 0; i < tokens_" + name + ".length; i++) {\n" +
                "            " + name + "[i] = Integer.parseInt(tokens_" + name + "[i]);\n" +
                "        }\n";
            case LONG_ARRAY ->
                "        String[] tokens_" + name + " = br.readLine().trim().split(\"\\\\s+\");\n" +
                "        long[] " + name + " = new long[tokens_" + name + ".length];\n" +
                "        for (int i = 0; i < tokens_" + name + ".length; i++) {\n" +
                "            " + name + "[i] = Long.parseLong(tokens_" + name + "[i]);\n" +
                "        }\n";
            case BOOLEAN_ARRAY ->
                "        String[] tokens_" + name + " = br.readLine().trim().split(\"\\\\s+\");\n" +
                "        boolean[] " + name + " = new boolean[tokens_" + name + ".length];\n" +
                "        for (int i = 0; i < tokens_" + name + ".length; i++) {\n" +
                "            " + name + "[i] = Boolean.parseBoolean(tokens_" + name + "[i]);\n" +
                "        }\n";
            case STRING_ARRAY ->
                "        int size_" + name + " = Integer.parseInt(br.readLine().trim());\n" +
                "        String[] " + name + " = new String[size_" + name + "];\n" +
                "        for (int i = 0; i < size_" + name + "; i++) {\n" +
                "            " + name + "[i] = br.readLine().trim();\n" +
                "        }\n";
            case INT_2D_ARRAY ->
                "        String[] dims_" + name + " = br.readLine().trim().split(\"\\\\s+\");\n" +
                "        int rows_" + name + " = Integer.parseInt(dims_" + name + "[0]);\n" +
                "        int cols_" + name + " = Integer.parseInt(dims_" + name + "[1]);\n" +
                "        int[][] " + name + " = new int[rows_" + name + "][cols_" + name + "];\n" +
                "        for (int i = 0; i < rows_" + name + "; i++) {\n" +
                "            String[] row_" + name + " = br.readLine().trim().split(\"\\\\s+\");\n" +
                "            for (int j = 0; j < cols_" + name + "; j++) {\n" +
                "                " + name + "[i][j] = Integer.parseInt(row_" + name + "[j]);\n" +
                "            }\n" +
                "        }\n";
            case LONG_2D_ARRAY ->
                "        String[] dims_" + name + " = br.readLine().trim().split(\"\\\\s+\");\n" +
                "        int rows_" + name + " = Integer.parseInt(dims_" + name + "[0]);\n" +
                "        int cols_" + name + " = Integer.parseInt(dims_" + name + "[1]);\n" +
                "        long[][] " + name + " = new long[rows_" + name + "][cols_" + name + "];\n" +
                "        for (int i = 0; i < rows_" + name + "; i++) {\n" +
                "            String[] row_" + name + " = br.readLine().trim().split(\"\\\\s+\");\n" +
                "            for (int j = 0; j < cols_" + name + "; j++) {\n" +
                "                " + name + "[i][j] = Long.parseLong(row_" + name + "[j]);\n" +
                "            }\n" +
                "        }\n";
            case BOOLEAN_2D_ARRAY ->
                "        String[] dims_" + name + " = br.readLine().trim().split(\"\\\\s+\");\n" +
                "        int rows_" + name + " = Integer.parseInt(dims_" + name + "[0]);\n" +
                "        int cols_" + name + " = Integer.parseInt(dims_" + name + "[1]);\n" +
                "        boolean[][] " + name + " = new boolean[rows_" + name + "][cols_" + name + "];\n" +
                "        for (int i = 0; i < rows_" + name + "; i++) {\n" +
                "            String[] row_" + name + " = br.readLine().trim().split(\"\\\\s+\");\n" +
                "            for (int j = 0; j < cols_" + name + "; j++) {\n" +
                "                " + name + "[i][j] = Boolean.parseBoolean(row_" + name + "[j]);\n" +
                "            }\n" +
                "        }\n";
            case STRING_2D_ARRAY ->
                "        String[] dims_" + name + " = br.readLine().trim().split(\"\\\\s+\");\n" +
                "        int rows_" + name + " = Integer.parseInt(dims_" + name + "[0]);\n" +
                "        int cols_" + name + " = Integer.parseInt(dims_" + name + "[1]);\n" +
                "        String[][] " + name + " = new String[rows_" + name + "][cols_" + name + "];\n" +
                "        for (int i = 0; i < rows_" + name + "; i++) {\n" +
                "            String[] row_" + name + " = br.readLine().trim().split(\"\\\\s+\");\n" +
                "            for (int j = 0; j < cols_" + name + "; j++) {\n" +
                "                " + name + "[i][j] = row_" + name + "[j];\n" +
                "            }\n" +
                "        }\n";
        };
    }

    private String generateJavaOutputCode(ValueType returnType) {
        return switch (returnType) {
            case INT, LONG, BOOLEAN, STRING -> "        System.out.println(result);\n";
            case INT_ARRAY, LONG_ARRAY, BOOLEAN_ARRAY ->
                "        for (int i = 0; i < result.length; i++) {\n" +
                "            if (i > 0) System.out.print(\" \");\n" +
                "            System.out.print(result[i]);\n" +
                "        }\n" +
                "        System.out.println();\n";
            case STRING_ARRAY ->
                "        System.out.println(result.length);\n" +
                "        for (String val : result) {\n" +
                "            System.out.println(val);\n" +
                "        }\n";
            case INT_2D_ARRAY, LONG_2D_ARRAY, BOOLEAN_2D_ARRAY ->
                "        System.out.println(result.length + \" \" + result[0].length);\n" +
                "        for (var row : result) {\n" +
                "            for (int i = 0; i < row.length; i++) {\n" +
                "                if (i > 0) System.out.print(\" \");\n" +
                "                System.out.print(row[i]);\n" +
                "            }\n" +
                "            System.out.println();\n" +
                "        }\n";
            case STRING_2D_ARRAY ->
                "        System.out.println(result.length + \" \" + result[0].length);\n" +
                "        for (String[] row : result) {\n" +
                "            System.out.println(String.join(\" \", row));\n" +
                "        }\n";
        };
    }

    private String valueTypeToJavaType(ValueType type) {
        return switch (type) {
            case INT -> "int";
            case LONG -> "long";
            case BOOLEAN -> "boolean";
            case STRING -> "String";
            case INT_ARRAY -> "int[]";
            case LONG_ARRAY -> "long[]";
            case BOOLEAN_ARRAY -> "boolean[]";
            case STRING_ARRAY -> "String[]";
            case INT_2D_ARRAY -> "int[][]";
            case LONG_2D_ARRAY -> "long[][]";
            case BOOLEAN_2D_ARRAY -> "boolean[][]";
            case STRING_2D_ARRAY -> "String[][]";
        };
    }

    // ── Python ────────────────────────────────────────────────────────────────

    private String generatePythonCode(
            String userCode, String functionName,
            List<ParameterSpec> params, ValueType returnType
    ) {
        StringBuilder code = new StringBuilder();

        code.append(userCode).append("\n\n");

        code.append("if __name__ == '__main__':\n");
        code.append("    import sys\n");
        code.append("    input_lines = sys.stdin.read().strip().split('\\n')\n");
        code.append("    line_idx = 0\n\n");

        for (ParameterSpec param : params) {
            code.append(generatePythonParameterParsing(param));
        }

        code.append("\n    solution = Solution()\n");
        code.append("    result = solution.").append(functionName).append("(");
        code.append(params.stream().map(ParameterSpec::name).collect(Collectors.joining(", ")));
        code.append(")\n\n");

        code.append(generatePythonOutputCode(returnType));

        log.debug("Generated Python code:\n{}", code);
        return code.toString();
    }

    private String generatePythonParameterParsing(ParameterSpec param) {
        String name = param.name();
        return switch (param.type()) {
            case INT -> "    " + name + " = int(input_lines[line_idx].split()[0])\n    line_idx += 1\n";
            case LONG -> "    " + name + " = int(input_lines[line_idx].split()[0])\n    line_idx += 1\n";
            case BOOLEAN -> "    " + name + " = input_lines[line_idx].strip().lower() == 'true'\n    line_idx += 1\n";
            case STRING -> "    " + name + " = input_lines[line_idx]\n    line_idx += 1\n";
            case INT_ARRAY, LONG_ARRAY ->
                "    " + name + " = list(map(int, input_lines[line_idx].split()))\n    line_idx += 1\n";
            case BOOLEAN_ARRAY ->
                "    " + name + " = [x.lower() == 'true' for x in input_lines[line_idx].split()]\n    line_idx += 1\n";
            case STRING_ARRAY ->
                "    size_" + name + " = int(input_lines[line_idx])\n" +
                "    line_idx += 1\n" +
                "    " + name + " = []\n" +
                "    for i in range(size_" + name + "):\n" +
                "        " + name + ".append(input_lines[line_idx])\n" +
                "        line_idx += 1\n";
            case INT_2D_ARRAY, LONG_2D_ARRAY ->
                "    tokens_" + name + " = input_lines[line_idx].split()\n" +
                "    rows_" + name + " = int(tokens_" + name + "[0])\n" +
                "    line_idx += 1\n" +
                "    " + name + " = []\n" +
                "    for i in range(rows_" + name + "):\n" +
                "        row = list(map(int, input_lines[line_idx].split()))\n" +
                "        " + name + ".append(row)\n" +
                "        line_idx += 1\n";
            case BOOLEAN_2D_ARRAY ->
                "    tokens_" + name + " = input_lines[line_idx].split()\n" +
                "    rows_" + name + " = int(tokens_" + name + "[0])\n" +
                "    line_idx += 1\n" +
                "    " + name + " = []\n" +
                "    for i in range(rows_" + name + "):\n" +
                "        row = [x.lower() == 'true' for x in input_lines[line_idx].split()]\n" +
                "        " + name + ".append(row)\n" +
                "        line_idx += 1\n";
            case STRING_2D_ARRAY ->
                "    tokens_" + name + " = input_lines[line_idx].split()\n" +
                "    rows_" + name + " = int(tokens_" + name + "[0])\n" +
                "    line_idx += 1\n" +
                "    " + name + " = []\n" +
                "    for i in range(rows_" + name + "):\n" +
                "        row = input_lines[line_idx].split()\n" +
                "        " + name + ".append(row)\n" +
                "        line_idx += 1\n";
        };
    }

    private String generatePythonOutputCode(ValueType returnType) {
        return switch (returnType) {
            case INT, LONG, STRING -> "    print(result)\n";
            case BOOLEAN -> "    print('true' if result else 'false')\n";
            case INT_ARRAY, LONG_ARRAY -> "    print(' '.join(map(str, result)))\n";
            case BOOLEAN_ARRAY -> "    print(' '.join('true' if x else 'false' for x in result))\n";
            case STRING_ARRAY ->
                "    print(len(result))\n" +
                "    for s in result:\n" +
                "        print(s)\n";
            case INT_2D_ARRAY, LONG_2D_ARRAY ->
                "    print(len(result), len(result[0]))\n" +
                "    for row in result:\n" +
                "        print(' '.join(map(str, row)))\n";
            case BOOLEAN_2D_ARRAY ->
                "    print(len(result), len(result[0]))\n" +
                "    for row in result:\n" +
                "        print(' '.join('true' if x else 'false' for x in row))\n";
            case STRING_2D_ARRAY ->
                "    print(len(result), len(result[0]))\n" +
                "    for row in result:\n" +
                "        print(' '.join(row))\n";
        };
    }

    // ── C++ ───────────────────────────────────────────────────────────────────

    private String generateCppCode(
            String userCode, String functionName,
            List<ParameterSpec> params, ValueType returnType
    ) {
        StringBuilder code = new StringBuilder();
        code.append("#include <sstream>\n");
        code.append("#include <iostream>\n");
        code.append("#include <vector>\n");
        code.append("#include <string>\n");
        code.append("using namespace std;\n\n");

        code.append(userCode).append("\n\n");

        code.append("int main() {\n");

        for (ParameterSpec param : params) {
            code.append(generateCppParameterParsing(param));
        }

        code.append("\n    Solution solution;\n");
        code.append("    ");
        code.append(valueTypeToCppType(returnType)).append(" result = ");
        code.append("solution.").append(functionName).append("(");
        code.append(params.stream().map(ParameterSpec::name).collect(Collectors.joining(", ")));
        code.append(");\n\n");

        code.append(generateCppOutputCode(returnType));

        code.append("\n    return 0;\n");
        code.append("}\n");

        log.debug("Generated C++ code:\n{}", code);
        return code.toString();
    }

    private String generateCppParameterParsing(ParameterSpec param) {
        String name = param.name();
        return switch (param.type()) {
            case INT ->
                "    int " + name + ";\n" +
                "    cin >> " + name + ";\n";
            case LONG ->
                "    long long " + name + ";\n" +
                "    cin >> " + name + ";\n";
            case BOOLEAN ->
                "    string boolStr_" + name + ";\n" +
                "    cin >> boolStr_" + name + ";\n" +
                "    bool " + name + " = (boolStr_" + name + " == \"true\");\n";
            case STRING ->
                "    string " + name + ";\n" +
                "    getline(cin, " + name + ");\n";
            case INT_ARRAY ->
                "    string line_" + name + ";\n" +
                "    getline(cin, line_" + name + ");\n" +
                "    istringstream iss_" + name + "(line_" + name + ");\n" +
                "    vector<int> " + name + ";\n" +
                "    { int val; while (iss_" + name + " >> val) " + name + ".push_back(val); }\n";
            case LONG_ARRAY ->
                "    string line_" + name + ";\n" +
                "    getline(cin, line_" + name + ");\n" +
                "    istringstream iss_" + name + "(line_" + name + ");\n" +
                "    vector<long long> " + name + ";\n" +
                "    { long long val; while (iss_" + name + " >> val) " + name + ".push_back(val); }\n";
            case BOOLEAN_ARRAY ->
                "    string line_" + name + ";\n" +
                "    getline(cin, line_" + name + ");\n" +
                "    istringstream iss_" + name + "(line_" + name + ");\n" +
                "    vector<bool> " + name + ";\n" +
                "    { string tok; while (iss_" + name + " >> tok) " + name + ".push_back(tok == \"true\"); }\n";
            case STRING_ARRAY ->
                "    int size_" + name + ";\n" +
                "    cin >> size_" + name + ";\n" +
                "    cin.ignore();\n" +
                "    vector<string> " + name + ";\n" +
                "    for (int i = 0; i < size_" + name + "; i++) {\n" +
                "        string s; getline(cin, s);\n" +
                "        " + name + ".push_back(s);\n" +
                "    }\n";
            case INT_2D_ARRAY ->
                "    int rows_" + name + ", cols_" + name + ";\n" +
                "    cin >> rows_" + name + " >> cols_" + name + ";\n" +
                "    vector<vector<int>> " + name + "(rows_" + name + ", vector<int>(cols_" + name + "));\n" +
                "    for (int i = 0; i < rows_" + name + "; i++) {\n" +
                "        for (int j = 0; j < cols_" + name + "; j++) {\n" +
                "            cin >> " + name + "[i][j];\n" +
                "        }\n" +
                "    }\n";
            case LONG_2D_ARRAY ->
                "    int rows_" + name + ", cols_" + name + ";\n" +
                "    cin >> rows_" + name + " >> cols_" + name + ";\n" +
                "    vector<vector<long long>> " + name + "(rows_" + name + ", vector<long long>(cols_" + name + "));\n" +
                "    for (int i = 0; i < rows_" + name + "; i++) {\n" +
                "        for (int j = 0; j < cols_" + name + "; j++) {\n" +
                "            cin >> " + name + "[i][j];\n" +
                "        }\n" +
                "    }\n";
            case BOOLEAN_2D_ARRAY ->
                "    int rows_" + name + ", cols_" + name + ";\n" +
                "    cin >> rows_" + name + " >> cols_" + name + ";\n" +
                "    vector<vector<bool>> " + name + "(rows_" + name + ", vector<bool>(cols_" + name + "));\n" +
                "    for (int i = 0; i < rows_" + name + "; i++) {\n" +
                "        for (int j = 0; j < cols_" + name + "; j++) {\n" +
                "            string tok; cin >> tok;\n" +
                "            " + name + "[i][j] = (tok == \"true\");\n" +
                "        }\n" +
                "    }\n";
            case STRING_2D_ARRAY ->
                "    int rows_" + name + ", cols_" + name + ";\n" +
                "    cin >> rows_" + name + " >> cols_" + name + ";\n" +
                "    cin.ignore();\n" +
                "    vector<vector<string>> " + name + "(rows_" + name + ");\n" +
                "    for (int i = 0; i < rows_" + name + "; i++) {\n" +
                "        string rowLine; getline(cin, rowLine);\n" +
                "        istringstream rowIss(rowLine);\n" +
                "        string tok;\n" +
                "        while (rowIss >> tok) " + name + "[i].push_back(tok);\n" +
                "    }\n";
        };
    }

    private String generateCppOutputCode(ValueType returnType) {
        return switch (returnType) {
            case INT, LONG, STRING -> "    cout << result << endl;\n";
            case BOOLEAN -> "    cout << (result ? \"true\" : \"false\") << endl;\n";
            case INT_ARRAY, LONG_ARRAY ->
                "    for (int i = 0; i < (int)result.size(); i++) {\n" +
                "        if (i > 0) cout << \" \";\n" +
                "        cout << result[i];\n" +
                "    }\n" +
                "    cout << endl;\n";
            case BOOLEAN_ARRAY ->
                "    for (int i = 0; i < (int)result.size(); i++) {\n" +
                "        if (i > 0) cout << \" \";\n" +
                "        cout << (result[i] ? \"true\" : \"false\");\n" +
                "    }\n" +
                "    cout << endl;\n";
            case STRING_ARRAY ->
                "    cout << result.size() << endl;\n" +
                "    for (const auto& s : result) cout << s << endl;\n";
            case INT_2D_ARRAY, LONG_2D_ARRAY ->
                "    cout << result.size() << \" \" << result[0].size() << endl;\n" +
                "    for (const auto& row : result) {\n" +
                "        for (int i = 0; i < (int)row.size(); i++) {\n" +
                "            if (i > 0) cout << \" \";\n" +
                "            cout << row[i];\n" +
                "        }\n" +
                "        cout << endl;\n" +
                "    }\n";
            case BOOLEAN_2D_ARRAY ->
                "    cout << result.size() << \" \" << result[0].size() << endl;\n" +
                "    for (const auto& row : result) {\n" +
                "        for (int i = 0; i < (int)row.size(); i++) {\n" +
                "            if (i > 0) cout << \" \";\n" +
                "            cout << (row[i] ? \"true\" : \"false\");\n" +
                "        }\n" +
                "        cout << endl;\n" +
                "    }\n";
            case STRING_2D_ARRAY ->
                "    cout << result.size() << \" \" << result[0].size() << endl;\n" +
                "    for (const auto& row : result) {\n" +
                "        for (int i = 0; i < (int)row.size(); i++) {\n" +
                "            if (i > 0) cout << \" \";\n" +
                "            cout << row[i];\n" +
                "        }\n" +
                "        cout << endl;\n" +
                "    }\n";
        };
    }

    private String valueTypeToCppType(ValueType type) {
        return switch (type) {
            case INT -> "int";
            case LONG -> "long long";
            case BOOLEAN -> "bool";
            case STRING -> "string";
            case INT_ARRAY -> "vector<int>";
            case LONG_ARRAY -> "vector<long long>";
            case BOOLEAN_ARRAY -> "vector<bool>";
            case STRING_ARRAY -> "vector<string>";
            case INT_2D_ARRAY -> "vector<vector<int>>";
            case LONG_2D_ARRAY -> "vector<vector<long long>>";
            case BOOLEAN_2D_ARRAY -> "vector<vector<bool>>";
            case STRING_2D_ARRAY -> "vector<vector<string>>";
        };
    }
}
