package com.solvemeup.smuworkerapi.worker.service;

import com.solvemeup.smuworkerapi.worker.dto.ParameterSpec;
import com.solvemeup.smuworkerapi.worker.enums.Language;
import com.solvemeup.smuworkerapi.worker.enums.ValueType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * Generates a driver that reads all test cases from one stdin stream and runs the
     * user function once per case inside a single process (LeetCode-style batch).
     * Input layout: first line is the case count T, followed by T concatenated case inputs.
     * For each case the driver prints a boundary line
     * "{SMU_BOUNDARY} {caseIndex} {OK|RE|TLE} {elapsedMillis}" (flushed) followed by the
     * case output. Per-case time limit is read from the TIME_LIMIT_MS env var.
     */
    public String generateBatchExecutableCode(
            Language language,
            String userCode,
            String functionName,
            List<ParameterSpec> parameters,
            ValueType returnType
    ) {
        return switch (language) {
            case JAVA -> generateJavaBatchCode(userCode, functionName, parameters, returnType);
            case PYTHON -> generatePythonBatchCode(userCode, functionName, parameters, returnType);
            case CPP -> generateCppBatchCode(userCode, functionName, parameters, returnType);
        };
    }

    private String generateJavaBatchCode(
            String userCode,
            String functionName,
            List<ParameterSpec> parameters,
            ValueType returnType
    ) {
        StringBuilder code = new StringBuilder("""
                import java.io.*;
                import java.util.*;
                import java.util.concurrent.*;

                """);
        code.append(userCode).append("\n\n");
        code.append("""
                public class Main {
                    static final String BOUNDARY = System.getenv("SMU_BOUNDARY") == null ? "" : System.getenv("SMU_BOUNDARY");
                    static final long TIME_LIMIT_MS = Long.parseLong(System.getenv().getOrDefault("TIME_LIMIT_MS", "2000"));
                    static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> {
                        Thread t = new Thread(r);
                        t.setDaemon(true);
                        return t;
                    });

                    static <R> R __run(Callable<R> task) throws Exception {
                        Future<R> f = POOL.submit(task);
                        try {
                            return f.get(TIME_LIMIT_MS, TimeUnit.MILLISECONDS);
                        } catch (ExecutionException ee) {
                            Throwable c = ee.getCause();
                            if (c instanceof Error er) throw er;
                            throw (Exception) c;
                        }
                    }

                    public static void main(String[] args) throws Exception {
                        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                        Solution solution = new Solution();
                        int __T = Integer.parseInt(br.readLine().trim());
                        for (int __tc = 0; __tc < __T; __tc++) {
                """);
        parameters.forEach(parameter -> code.append(generateJavaParameterParsing(parameter)));
        code.append("            long __start = System.nanoTime();\n");
        code.append("            try {\n");
        code.append("                ").append(javaType(returnType)).append(" result = __run(() -> solution.")
                .append(functionName).append("(").append(parameterNames(parameters)).append("));\n");
        code.append("                long __ms = (System.nanoTime() - __start) / 1000000L;\n");
        code.append("                System.out.println(BOUNDARY + \" \" + __tc + \" OK \" + __ms);\n");
        code.append(generateJavaOutputCode(returnType));
        code.append("                System.out.println();\n");
        code.append("                System.out.flush();\n");
        code.append("            } catch (TimeoutException __te) {\n");
        code.append("                System.out.println(BOUNDARY + \" \" + __tc + \" TLE \" + TIME_LIMIT_MS);\n");
        code.append("                System.out.flush();\n");
        code.append("                Runtime.getRuntime().halt(0);\n");
        code.append("            } catch (Throwable __e) {\n");
        code.append("                long __ms = (System.nanoTime() - __start) / 1000000L;\n");
        code.append("                System.out.println(BOUNDARY + \" \" + __tc + \" RE \" + __ms);\n");
        code.append("                System.out.flush();\n");
        code.append("            }\n");
        code.append("        }\n");
        code.append("        System.exit(0);\n");
        code.append("    }\n}\n");
        return code.toString();
    }

    private String generatePythonBatchCode(
            String userCode,
            String functionName,
            List<ParameterSpec> parameters,
            ValueType returnType
    ) {
        String argsTuple = parameters.isEmpty()
                ? "()"
                : "(" + parameterNames(parameters) + ",)";

        StringBuilder parsing = new StringBuilder();
        parameters.forEach(parameter -> parsing.append(generatePythonParameterParsing(parameter)));
        String parsingBody = parsing.length() == 0
                ? "        pass\n"
                : parsing.toString().indent(4);
        String outputBody = generatePythonOutputCode(TypeShape.from(returnType)).indent(8);

        StringBuilder code = new StringBuilder(userCode).append("\n\n\n");
        code.append("if __name__ == '__main__':\n");
        code.append("    import sys, os, signal, time\n\n");
        code.append("    BOUNDARY = os.environ.get('SMU_BOUNDARY', '')\n");
        code.append("    TIME_LIMIT_MS = int(os.environ.get('TIME_LIMIT_MS', '2000'))\n\n");
        code.append("    class _TLE(Exception):\n        pass\n\n");
        code.append("    def _on_alarm(signum, frame):\n        raise _TLE()\n\n");
        code.append("    signal.signal(signal.SIGALRM, _on_alarm)\n\n");
        code.append("    input_lines = sys.stdin.read().splitlines()\n");
        code.append("    line_idx = 0\n");
        code.append("    _T = int(input_lines[line_idx]); line_idx += 1\n");
        code.append("    solution = Solution()\n\n");
        code.append("    def _parse_case(line_idx):\n");
        code.append(parsingBody);
        code.append("        return (line_idx, ").append(argsTuple).append(")\n\n");
        code.append("    for _tc in range(_T):\n");
        code.append("        line_idx, _args = _parse_case(line_idx)\n");
        code.append("        _start = time.perf_counter()\n");
        code.append("        try:\n");
        code.append("            signal.setitimer(signal.ITIMER_REAL, TIME_LIMIT_MS / 1000.0)\n");
        code.append("            result = solution.").append(functionName).append("(*_args)\n");
        code.append("            signal.setitimer(signal.ITIMER_REAL, 0)\n");
        code.append("            _ms = int((time.perf_counter() - _start) * 1000)\n");
        code.append("            print(f\"{BOUNDARY} {_tc} OK {_ms}\")\n");
        code.append(outputBody);
        code.append("            print()\n");
        code.append("            sys.stdout.flush()\n");
        code.append("        except _TLE:\n");
        code.append("            print(f\"{BOUNDARY} {_tc} TLE {TIME_LIMIT_MS}\")\n");
        code.append("            sys.stdout.flush()\n");
        code.append("            os._exit(0)\n");
        code.append("        except Exception:\n");
        code.append("            signal.setitimer(signal.ITIMER_REAL, 0)\n");
        code.append("            _ms = int((time.perf_counter() - _start) * 1000)\n");
        code.append("            print(f\"{BOUNDARY} {_tc} RE {_ms}\")\n");
        code.append("            sys.stdout.flush()\n");
        return code.toString();
    }

    private String generateCppBatchCode(
            String userCode,
            String functionName,
            List<ParameterSpec> parameters,
            ValueType returnType
    ) {
        StringBuilder code = new StringBuilder("""
                #include <iostream>
                #include <sstream>
                #include <string>
                #include <type_traits>
                #include <vector>
                #include <csignal>
                #include <cstdlib>
                #include <cstring>
                #include <unistd.h>
                #include <sys/time.h>
                #include <chrono>
                using namespace std;

                """);
        code.append(userCode).append("\n\n");
        code.append("""
                static string __TLE_MSG;

                static void __onAlarm(int) {
                    ssize_t __n = write(1, __TLE_MSG.c_str(), __TLE_MSG.size());
                    (void) __n;
                    _exit(0);
                }

                int main() {
                    const char* __b = getenv("SMU_BOUNDARY");
                    string __BOUNDARY = __b ? string(__b) : string("");
                    const char* __t = getenv("TIME_LIMIT_MS");
                    long __LIMIT = __t ? atol(__t) : 2000;
                    signal(SIGALRM, __onAlarm);

                    Solution solution;
                    int __T;
                    if (!(cin >> __T)) return 0;
                    for (int __tc = 0; __tc < __T; __tc++) {
                """);
        parameters.forEach(parameter -> code.append(generateCppParameterParsing(parameter)));
        code.append("        __TLE_MSG = __BOUNDARY + \" \" + to_string(__tc) + \" TLE \" + to_string(__LIMIT) + \"\\n\";\n");
        code.append("        auto __start = chrono::steady_clock::now();\n");
        code.append("        struct itimerval __it; memset(&__it, 0, sizeof(__it));\n");
        code.append("        __it.it_value.tv_sec = __LIMIT / 1000;\n");
        code.append("        __it.it_value.tv_usec = (__LIMIT % 1000) * 1000;\n");
        code.append("        setitimer(ITIMER_REAL, &__it, nullptr);\n");
        code.append("        try {\n");
        code.append("            ").append(cppType(returnType)).append(" result = solution.")
                .append(functionName).append("(").append(parameterNames(parameters)).append(");\n");
        code.append("            struct itimerval __z; memset(&__z, 0, sizeof(__z));\n");
        code.append("            setitimer(ITIMER_REAL, &__z, nullptr);\n");
        code.append("            long long __ms = chrono::duration_cast<chrono::milliseconds>(chrono::steady_clock::now() - __start).count();\n");
        code.append("            cout << __BOUNDARY << \" \" << __tc << \" OK \" << __ms << \"\\n\";\n");
        code.append(generateCppOutputCode(TypeShape.from(returnType)));
        code.append("            cout << \"\\n\";\n");
        code.append("            cout.flush();\n");
        code.append("        } catch (...) {\n");
        code.append("            struct itimerval __z; memset(&__z, 0, sizeof(__z));\n");
        code.append("            setitimer(ITIMER_REAL, &__z, nullptr);\n");
        code.append("            long long __ms = chrono::duration_cast<chrono::milliseconds>(chrono::steady_clock::now() - __start).count();\n");
        code.append("            cout << __BOUNDARY << \" \" << __tc << \" RE \" << __ms << \"\\n\";\n");
        code.append("            cout.flush();\n");
        code.append("        }\n");
        code.append("    }\n");
        code.append("    return 0;\n}\n");
        return code.toString();
    }

    private String generateJavaCode(
            String userCode,
            String functionName,
            List<ParameterSpec> parameters,
            ValueType returnType
    ) {
        StringBuilder code = new StringBuilder("""
                import java.io.*;
                import java.lang.reflect.Array;
                import java.util.*;

                """);
        code.append(userCode).append("\n\n");
        code.append("""
                public class Main {
                    private static String toJson(Object value) {
                        if (value == null) return "null";
                        if (value instanceof String || value instanceof Character) {
                            return "\\\"" + escapeJson(value.toString()) + "\\\"";
                        }
                        if (value instanceof Boolean || value instanceof Number) {
                            return value.toString();
                        }
                        if (value.getClass().isArray()) {
                            StringBuilder json = new StringBuilder("[");
                            for (int i = 0; i < Array.getLength(value); i++) {
                                if (i > 0) json.append(',');
                                json.append(toJson(Array.get(value, i)));
                            }
                            return json.append(']').toString();
                        }
                        throw new IllegalArgumentException("Unsupported result type: " + value.getClass());
                    }

                    private static String escapeJson(String value) {
                        return value.replace("\\\\", "\\\\\\\\")
                                .replace("\\\"", "\\\\\\\"")
                                .replace("\\n", "\\\\n")
                                .replace("\\r", "\\\\r")
                                .replace("\\t", "\\\\t");
                    }

                    public static void main(String[] args) throws Exception {
                        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                        Solution solution = new Solution();
                """);
        parameters.forEach(parameter -> code.append(generateJavaParameterParsing(parameter)));
        code.append("        ").append(javaType(returnType)).append(" result = solution.")
                .append(functionName).append("(").append(parameterNames(parameters)).append(");\n")
                .append(generateJavaOutputCode(returnType))
                .append("    }\n")
                .append("}\n");
        return code.toString();
    }

    private String generateJavaParameterParsing(ParameterSpec parameter) {
        String name = parameter.name();
        TypeShape shape = TypeShape.from(parameter.type());
        if (shape.dimensions() == 0) {
            return "        " + javaType(parameter.type()) + " " + name + " = "
                    + javaScalarParser(shape.baseType(), "br.readLine().trim()") + ";\n";
        }
        if (parameter.type() == ValueType.STRING_ARRAY) {
            return """
                            int size_%1$s = Integer.parseInt(br.readLine().trim());
                            String[] %1$s = new String[size_%1$s];
                            for (int i = 0; i < size_%1$s; i++) %1$s[i] = br.readLine();
                    """.formatted(name);
        }
        if (shape.dimensions() == 1) {
            return generateJavaOneDimensionalParser(name, shape);
        }
        if (shape.dimensions() == 2) {
            return generateJavaTwoDimensionalParser(name, shape);
        }
        return generateJavaThreeDimensionalParser(name, shape);
    }

    private String generateJavaOneDimensionalParser(String name, TypeShape shape) {
        return """
                        String line_%1$s = br.readLine().trim();
                        String[] tokens_%1$s = line_%1$s.isEmpty() ? new String[0] : line_%1$s.split("\\s+");
                        %2$s %1$s = new %3$s[tokens_%1$s.length];
                        for (int i = 0; i < tokens_%1$s.length; i++) {
                            %1$s[i] = %4$s;
                        }
                """.formatted(
                name,
                javaType(shape.originalType()),
                javaBaseType(shape.baseType()),
                javaScalarParser(shape.baseType(), "tokens_" + name + "[i]")
        );
    }

    private String generateJavaTwoDimensionalParser(String name, TypeShape shape) {
        return """
                        String[] dims_%1$s = br.readLine().trim().split("\\s+");
                        int rows_%1$s = Integer.parseInt(dims_%1$s[0]);
                        int cols_%1$s = Integer.parseInt(dims_%1$s[1]);
                        %2$s %1$s = new %3$s[rows_%1$s][cols_%1$s];
                        for (int i = 0; i < rows_%1$s; i++) {
                            String[] row_%1$s = br.readLine().trim().split("\\s+");
                            for (int j = 0; j < cols_%1$s; j++) {
                                %1$s[i][j] = %4$s;
                            }
                        }
                """.formatted(
                name,
                javaType(shape.originalType()),
                javaBaseType(shape.baseType()),
                javaScalarParser(shape.baseType(), "row_" + name + "[j]")
        );
    }

    private String generateJavaThreeDimensionalParser(String name, TypeShape shape) {
        return """
                        String[] dims_%1$s = br.readLine().trim().split("\\s+");
                        int depth_%1$s = Integer.parseInt(dims_%1$s[0]);
                        int rows_%1$s = Integer.parseInt(dims_%1$s[1]);
                        int cols_%1$s = Integer.parseInt(dims_%1$s[2]);
                        %2$s %1$s = new %3$s[depth_%1$s][rows_%1$s][cols_%1$s];
                        for (int d = 0; d < depth_%1$s; d++) {
                            for (int i = 0; i < rows_%1$s; i++) {
                                String[] row_%1$s = br.readLine().trim().split("\\s+");
                                for (int j = 0; j < cols_%1$s; j++) {
                                    %1$s[d][i][j] = %4$s;
                                }
                            }
                        }
                """.formatted(
                name,
                javaType(shape.originalType()),
                javaBaseType(shape.baseType()),
                javaScalarParser(shape.baseType(), "row_" + name + "[j]")
        );
    }

    private String generatePythonCode(
            String userCode,
            String functionName,
            List<ParameterSpec> parameters,
            ValueType returnType
    ) {
        StringBuilder code = new StringBuilder(userCode).append("\n\n").append("""
                if __name__ == '__main__':
                    import json
                    import sys
                    input_lines = sys.stdin.read().splitlines()
                    line_idx = 0
                """);
        parameters.forEach(parameter -> code.append(generatePythonParameterParsing(parameter)));
        code.append("    solution = Solution()\n")
                .append("    result = solution.").append(functionName).append("(")
                .append(parameterNames(parameters)).append(")\n")
                .append(generatePythonOutputCode(TypeShape.from(returnType)));
        return code.toString();
    }

    private String generatePythonParameterParsing(ParameterSpec parameter) {
        String name = parameter.name();
        TypeShape shape = TypeShape.from(parameter.type());
        if (shape.dimensions() == 0) {
            return "    " + name + " = " + pythonScalarParser(shape.baseType(), "input_lines[line_idx]")
                    + "\n    line_idx += 1\n";
        }
        if (parameter.type() == ValueType.STRING_ARRAY) {
            return """
                    size_%1$s = int(input_lines[line_idx])
                    line_idx += 1
                    %1$s = input_lines[line_idx:line_idx + size_%1$s]
                    line_idx += size_%1$s
                    """.formatted(name).indent(4);
        }
        if (shape.dimensions() == 1) {
            return ("    " + name + " = [" + pythonScalarParser(shape.baseType(), "value")
                    + " for value in input_lines[line_idx].split()]\n    line_idx += 1\n");
        }
        if (shape.dimensions() == 2) {
            return """
                    dims_%1$s = list(map(int, input_lines[line_idx].split()))
                    line_idx += 1
                    %1$s = []
                    for _ in range(dims_%1$s[0]):
                        %1$s.append([%2$s for value in input_lines[line_idx].split()])
                        line_idx += 1
                    """.formatted(name, pythonScalarParser(shape.baseType(), "value")).indent(4);
        }
        return """
                dims_%1$s = list(map(int, input_lines[line_idx].split()))
                line_idx += 1
                %1$s = []
                for _ in range(dims_%1$s[0]):
                    matrix = []
                    for _ in range(dims_%1$s[1]):
                        matrix.append([%2$s for value in input_lines[line_idx].split()])
                        line_idx += 1
                    %1$s.append(matrix)
                """.formatted(name, pythonScalarParser(shape.baseType(), "value")).indent(4);
    }

    private String generateCppCode(
            String userCode,
            String functionName,
            List<ParameterSpec> parameters,
            ValueType returnType
    ) {
        StringBuilder code = new StringBuilder("""
                #include <iostream>
                #include <sstream>
                #include <string>
                #include <type_traits>
                #include <vector>
                using namespace std;

                string escapeJson(const string& value) {
                    string result;
                    for (char c : value) {
                        if (c == '\\\\' || c == '"') result += '\\\\';
                        if (c == '\\n') result += "\\\\n";
                        else if (c == '\\r') result += "\\\\r";
                        else if (c == '\\t') result += "\\\\t";
                        else result += c;
                    }
                    return result;
                }

                void printJson(const string& value) { cout << '"' << escapeJson(value) << '"'; }
                void printJson(char value) { cout << '"' << escapeJson(string(1, value)) << '"'; }
                void printJson(bool value) { cout << (value ? "true" : "false"); }
                template <typename T, enable_if_t<is_arithmetic_v<T> && !is_same_v<T, bool>, int> = 0>
                void printJson(T value) { cout << value; }
                template <typename T>
                void printJson(const vector<T>& values) {
                    cout << '[';
                    for (size_t i = 0; i < values.size(); i++) {
                        if (i > 0) cout << ',';
                        printJson(values[i]);
                    }
                    cout << ']';
                }

                """);
        code.append(userCode).append("\n\nint main() {\n");
        parameters.forEach(parameter -> code.append(generateCppParameterParsing(parameter)));
        code.append("    Solution solution;\n    ").append(cppType(returnType)).append(" result = solution.")
                .append(functionName).append("(").append(parameterNames(parameters)).append(");\n")
                .append(generateCppOutputCode(TypeShape.from(returnType)))
                .append("    return 0;\n}\n");
        return code.toString();
    }

    private String generateJavaOutputCode(ValueType returnType) {
        TypeShape shape = TypeShape.from(returnType);
        if (shape.dimensions() == 0) {
            return "        System.out.print(result);\n";
        }
        if (returnType == ValueType.STRING_ARRAY) {
            return """
                            System.out.println(result.length);
                            for (String value : result) System.out.println(value);
                    """;
        }
        if (shape.dimensions() == 1) {
            return """
                            for (int i = 0; i < result.length; i++) {
                                if (i > 0) System.out.print(" ");
                                System.out.print(result[i]);
                            }
                    """;
        }
        if (shape.dimensions() == 2) {
            return """
                            int resultRows = result.length;
                            int resultCols = resultRows == 0 ? 0 : result[0].length;
                            System.out.println(resultRows + " " + resultCols);
                            for (var row : result) {
                                for (int i = 0; i < row.length; i++) {
                                    if (i > 0) System.out.print(" ");
                                    System.out.print(row[i]);
                                }
                                System.out.println();
                            }
                    """;
        }
        return """
                        int resultDepth = result.length;
                        int resultRows = resultDepth == 0 ? 0 : result[0].length;
                        int resultCols = resultRows == 0 ? 0 : result[0][0].length;
                        System.out.println(resultDepth + " " + resultRows + " " + resultCols);
                        for (var matrix : result) {
                            for (var row : matrix) {
                                for (int i = 0; i < row.length; i++) {
                                    if (i > 0) System.out.print(" ");
                                    System.out.print(row[i]);
                                }
                                System.out.println();
                            }
                        }
                """;
    }

    private String generatePythonOutputCode(TypeShape shape) {
        if (shape.dimensions() == 0) {
            if (shape.baseType() == BaseType.BOOLEAN) {
                return "    print('true' if result else 'false', end='')\n";
            }
            return "    print(result, end='')\n";
        }
        if (shape.originalType() == ValueType.STRING_ARRAY) {
            return """
                        print(len(result))
                        for value in result:
                            print(value)
                    """;
        }
        if (shape.dimensions() == 1) {
            String value = shape.baseType() == BaseType.BOOLEAN
                    ? "('true' if value else 'false')"
                    : "str(value)";
            return "    print(' '.join(" + value + " for value in result), end='')\n";
        }
        String value = shape.baseType() == BaseType.BOOLEAN
                ? "('true' if value else 'false')"
                : "str(value)";
        if (shape.dimensions() == 2) {
            return """
                        print(len(result), len(result[0]) if result else 0)
                        for row in result:
                            print(' '.join(%s for value in row))
                    """.formatted(value);
        }
        return """
                    print(len(result), len(result[0]) if result else 0,
                          len(result[0][0]) if result and result[0] else 0)
                    for matrix in result:
                        for row in matrix:
                            print(' '.join(%s for value in row))
                """.formatted(value);
    }

    private String generateCppOutputCode(TypeShape shape) {
        if (shape.dimensions() == 0) {
            if (shape.baseType() == BaseType.BOOLEAN) {
                return "    cout << (result ? \"true\" : \"false\");\n";
            }
            return "    cout << result;\n";
        }
        if (shape.originalType() == ValueType.STRING_ARRAY) {
            return """
                        cout << result.size() << endl;
                        for (const auto& value : result) cout << value << endl;
                    """;
        }
        String printValue = shape.baseType() == BaseType.BOOLEAN
                ? "cout << (value ? \"true\" : \"false\");"
                : "cout << value;";
        if (shape.dimensions() == 1) {
            return """
                        for (size_t i = 0; i < result.size(); i++) {
                            if (i > 0) cout << " ";
                            auto value = result[i];
                            %s
                        }
                    """.formatted(printValue);
        }
        if (shape.dimensions() == 2) {
            return """
                        cout << result.size() << " " << (result.empty() ? 0 : result[0].size()) << endl;
                        for (const auto& row : result) {
                            for (size_t i = 0; i < row.size(); i++) {
                                if (i > 0) cout << " ";
                                auto value = row[i];
                                %s
                            }
                            cout << endl;
                        }
                    """.formatted(printValue);
        }
        return """
                    cout << result.size() << " "
                         << (result.empty() ? 0 : result[0].size()) << " "
                         << (result.empty() || result[0].empty() ? 0 : result[0][0].size()) << endl;
                    for (const auto& matrix : result) {
                        for (const auto& row : matrix) {
                            for (size_t i = 0; i < row.size(); i++) {
                                if (i > 0) cout << " ";
                                auto value = row[i];
                                %s
                            }
                            cout << endl;
                        }
                    }
                """.formatted(printValue);
    }

    private String generateCppParameterParsing(ParameterSpec parameter) {
        String name = parameter.name();
        TypeShape shape = TypeShape.from(parameter.type());
        if (shape.dimensions() == 0) {
            if (shape.baseType() == BaseType.STRING) {
                return "    string " + name + ";\n    getline(cin >> ws, " + name + ");\n";
            }
            if (shape.baseType() == BaseType.BOOLEAN) {
                return "    string raw_" + name + ";\n    cin >> raw_" + name
                        + ";\n    bool " + name + " = raw_" + name + " == \"true\";\n";
            }
            return "    " + cppType(parameter.type()) + " " + name + ";\n    cin >> " + name + ";\n";
        }
        if (parameter.type() == ValueType.STRING_ARRAY) {
            return """
                        int size_%1$s;
                        cin >> size_%1$s;
                        cin.ignore();
                        vector<string> %1$s(size_%1$s);
                        for (int i = 0; i < size_%1$s; i++) getline(cin, %1$s[i]);
                    """.formatted(name);
        }
        if (shape.dimensions() == 1) {
            return """
                        string line_%1$s;
                        getline(cin >> ws, line_%1$s);
                        istringstream stream_%1$s(line_%1$s);
                        %2$s %1$s;
                        string token_%1$s;
                        while (stream_%1$s >> token_%1$s) %1$s.push_back(%3$s);
                    """.formatted(name, cppType(parameter.type()), cppScalarParser(shape.baseType(), "token_" + name));
        }
        if (shape.dimensions() == 2) {
            return """
                        int rows_%1$s, cols_%1$s;
                        cin >> rows_%1$s >> cols_%1$s;
                        %2$s %1$s(rows_%1$s, %3$s(cols_%1$s));
                        for (int i = 0; i < rows_%1$s; i++) {
                            for (int j = 0; j < cols_%1$s; j++) {
                                string token; cin >> token;
                                %1$s[i][j] = %4$s;
                            }
                        }
                    """.formatted(
                    name,
                    cppType(parameter.type()),
                    cppVectorType(shape.baseType(), 1),
                    cppScalarParser(shape.baseType(), "token")
            );
        }
        return """
                    int depth_%1$s, rows_%1$s, cols_%1$s;
                    cin >> depth_%1$s >> rows_%1$s >> cols_%1$s;
                    %2$s %1$s(depth_%1$s, %3$s(rows_%1$s, %4$s(cols_%1$s)));
                    for (int d = 0; d < depth_%1$s; d++) {
                        for (int i = 0; i < rows_%1$s; i++) {
                            for (int j = 0; j < cols_%1$s; j++) {
                                string token; cin >> token;
                                %1$s[d][i][j] = %5$s;
                            }
                        }
                    }
                """.formatted(
                name,
                cppType(parameter.type()),
                cppVectorType(shape.baseType(), 2),
                cppVectorType(shape.baseType(), 1),
                cppScalarParser(shape.baseType(), "token")
        );
    }

    private String javaScalarParser(BaseType type, String value) {
        return switch (type) {
            case INT -> "Integer.parseInt(" + value + ")";
            case LONG -> "Long.parseLong(" + value + ")";
            case BOOLEAN -> "Boolean.parseBoolean(" + value + ")";
            case CHAR -> value + ".charAt(0)";
            case DOUBLE -> "Double.parseDouble(" + value + ")";
            case STRING -> value;
        };
    }

    private String pythonScalarParser(BaseType type, String value) {
        return switch (type) {
            case INT, LONG -> "int(" + value + ")";
            case BOOLEAN -> value + ".lower() == 'true'";
            case DOUBLE -> "float(" + value + ")";
            case CHAR, STRING -> value;
        };
    }

    private String cppScalarParser(BaseType type, String value) {
        return switch (type) {
            case INT -> "stoi(" + value + ")";
            case LONG -> "stoll(" + value + ")";
            case BOOLEAN -> value + " == \"true\"";
            case CHAR -> value + "[0]";
            case DOUBLE -> "stod(" + value + ")";
            case STRING -> value;
        };
    }

    private String javaType(ValueType type) {
        TypeShape shape = TypeShape.from(type);
        return javaBaseType(shape.baseType()) + "[]".repeat(shape.dimensions());
    }

    private String javaBaseType(BaseType type) {
        return switch (type) {
            case INT -> "int";
            case LONG -> "long";
            case BOOLEAN -> "boolean";
            case CHAR -> "char";
            case DOUBLE -> "double";
            case STRING -> "String";
        };
    }

    private String cppType(ValueType type) {
        TypeShape shape = TypeShape.from(type);
        return cppVectorType(shape.baseType(), shape.dimensions());
    }

    private String cppVectorType(BaseType baseType, int dimensions) {
        String type = switch (baseType) {
            case INT -> "int";
            case LONG -> "long long";
            case BOOLEAN -> "bool";
            case CHAR -> "char";
            case DOUBLE -> "double";
            case STRING -> "string";
        };
        for (int i = 0; i < dimensions; i++) {
            type = "vector<" + type + ">";
        }
        return type;
    }

    private String parameterNames(List<ParameterSpec> parameters) {
        return parameters.stream().map(ParameterSpec::name).collect(Collectors.joining(", "));
    }

    private enum BaseType {
        INT, LONG, BOOLEAN, CHAR, DOUBLE, STRING
    }

    private record TypeShape(ValueType originalType, BaseType baseType, int dimensions) {
        private static TypeShape from(ValueType type) {
            String name = type.name();
            int dimensions = name.endsWith("_3D_ARRAY") ? 3
                    : name.endsWith("_2D_ARRAY") ? 2
                    : name.endsWith("_ARRAY") ? 1 : 0;
            String baseName = name
                    .replace("_3D_ARRAY", "")
                    .replace("_2D_ARRAY", "")
                    .replace("_ARRAY", "");
            return new TypeShape(type, BaseType.valueOf(baseName), dimensions);
        }
    }
}
