package com.solvemeup.smuworkerapi.worker.service;

import com.solvemeup.smuworkerapi.worker.dto.ParameterSpec;
import com.solvemeup.smuworkerapi.worker.enums.Language;
import com.solvemeup.smuworkerapi.worker.enums.ValueType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Java 파이프라인 정확성 검증
 *
 * arguments → toStdinInput() → stdin
 *                                      ↓
 *                     generateExecutableCode() → Java 코드
 *                                      ↓
 *                                javac + java 실행 → output 검증
 */
class CodeGeneratorJavaTest {

    private CodeGenerator codeGenerator;
    private ArgumentConverter argumentConverter;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        codeGenerator = new CodeGenerator();
        argumentConverter = new ArgumentConverter();
    }

    // ── 스칼라 타입 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT → INT: 두 배")
    void int_to_int() throws Exception {
        assertPipeline(
                "class Solution { public int f(int n) { return n * 2; } }",
                "f",
                List.of(spec("n", ValueType.INT)),
                ValueType.INT,
                args("5"),
                "10"
        );
    }

    @Test
    @DisplayName("LONG → LONG: +1")
    void long_to_long() throws Exception {
        assertPipeline(
                "class Solution { public long f(long n) { return n + 1L; } }",
                "f",
                List.of(spec("n", ValueType.LONG)),
                ValueType.LONG,
                args("9999999999"),
                "10000000000"
        );
    }

    @Test
    @DisplayName("BOOLEAN → BOOLEAN: negate")
    void boolean_to_boolean() throws Exception {
        assertPipeline(
                "class Solution { public boolean f(boolean b) { return !b; } }",
                "f",
                List.of(spec("b", ValueType.BOOLEAN)),
                ValueType.BOOLEAN,
                args("true"),
                "false"
        );
    }

    @Test
    @DisplayName("STRING → STRING: toUpperCase")
    void string_to_string() throws Exception {
        assertPipeline(
                "class Solution { public String f(String s) { return s.toUpperCase(); } }",
                "f",
                List.of(spec("s", ValueType.STRING)),
                ValueType.STRING,
                args("hello"),
                "HELLO"
        );
    }

    // ── 1D 배열 타입 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT_ARRAY → INT: 합계")
    void intArray_to_int() throws Exception {
        assertPipeline(
                "class Solution { public int f(int[] a) { int s=0; for(int x:a) s+=x; return s; } }",
                "f",
                List.of(spec("a", ValueType.INT_ARRAY)),
                ValueType.INT,
                args("[1,2,3,4,5]"),
                "15"
        );
    }

    @Test
    @DisplayName("INT_ARRAY → INT_ARRAY: identity")
    void intArray_to_intArray() throws Exception {
        assertPipeline(
                "class Solution { public int[] f(int[] a) { return a; } }",
                "f",
                List.of(spec("a", ValueType.INT_ARRAY)),
                ValueType.INT_ARRAY,
                args("[1,2,3]"),
                "1 2 3"
        );
    }

    @Test
    @DisplayName("LONG_ARRAY → LONG: 합계")
    void longArray_to_long() throws Exception {
        assertPipeline(
                "class Solution { public long f(long[] a) { long s=0; for(long x:a) s+=x; return s; } }",
                "f",
                List.of(spec("a", ValueType.LONG_ARRAY)),
                ValueType.LONG,
                args("[1000000000,2000000000,3000000000]"),
                "6000000000"
        );
    }

    @Test
    @DisplayName("LONG_ARRAY → LONG_ARRAY: identity")
    void longArray_to_longArray() throws Exception {
        assertPipeline(
                "class Solution { public long[] f(long[] a) { return a; } }",
                "f",
                List.of(spec("a", ValueType.LONG_ARRAY)),
                ValueType.LONG_ARRAY,
                args("[10,20,30]"),
                "10 20 30"
        );
    }

    @Test
    @DisplayName("BOOLEAN_ARRAY → BOOLEAN: anyTrue")
    void booleanArray_to_boolean() throws Exception {
        assertPipeline(
                "class Solution { public boolean f(boolean[] a) { for(boolean b:a) if(b) return true; return false; } }",
                "f",
                List.of(spec("a", ValueType.BOOLEAN_ARRAY)),
                ValueType.BOOLEAN,
                args("[false,false,true]"),
                "true"
        );
    }

    @Test
    @DisplayName("BOOLEAN_ARRAY → BOOLEAN_ARRAY: identity")
    void booleanArray_to_booleanArray() throws Exception {
        assertPipeline(
                "class Solution { public boolean[] f(boolean[] a) { return a; } }",
                "f",
                List.of(spec("a", ValueType.BOOLEAN_ARRAY)),
                ValueType.BOOLEAN_ARRAY,
                args("[true,false,true]"),
                "true false true"
        );
    }

    @Test
    @DisplayName("STRING_ARRAY → STRING: join")
    void stringArray_to_string() throws Exception {
        String innerJson = mapper.writeValueAsString(new String[]{"a", "b", "c"});
        assertPipeline(
                "class Solution { public String f(String[] a) { return String.join(\",\", a); } }",
                "f",
                List.of(spec("a", ValueType.STRING_ARRAY)),
                ValueType.STRING,
                args(innerJson),
                "a,b,c"
        );
    }

    @Test
    @DisplayName("STRING_ARRAY → STRING_ARRAY: identity")
    void stringArray_to_stringArray() throws Exception {
        String innerJson = mapper.writeValueAsString(new String[]{"hello", "world"});
        assertPipeline(
                "class Solution { public String[] f(String[] a) { return a; } }",
                "f",
                List.of(spec("a", ValueType.STRING_ARRAY)),
                ValueType.STRING_ARRAY,
                args(innerJson),
                "2\nhello\nworld"
        );
    }

    // ── 2D 배열 타입 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT_2D_ARRAY → INT: 전체 합")
    void int2dArray_to_int() throws Exception {
        assertPipeline(
                "class Solution { public int f(int[][] m) { int s=0; for(int[] r:m) for(int x:r) s+=x; return s; } }",
                "f",
                List.of(spec("m", ValueType.INT_2D_ARRAY)),
                ValueType.INT,
                args("[[1,2],[3,4],[5,6]]"),
                "21"
        );
    }

    @Test
    @DisplayName("INT_2D_ARRAY → INT_2D_ARRAY: identity")
    void int2dArray_to_int2dArray() throws Exception {
        assertPipeline(
                "class Solution { public int[][] f(int[][] m) { return m; } }",
                "f",
                List.of(spec("m", ValueType.INT_2D_ARRAY)),
                ValueType.INT_2D_ARRAY,
                args("[[1,2],[3,4]]"),
                "2 2\n1 2\n3 4"
        );
    }

    @Test
    @DisplayName("LONG_2D_ARRAY → LONG: 전체 합")
    void long2dArray_to_long() throws Exception {
        assertPipeline(
                "class Solution { public long f(long[][] m) { long s=0; for(long[] r:m) for(long x:r) s+=x; return s; } }",
                "f",
                List.of(spec("m", ValueType.LONG_2D_ARRAY)),
                ValueType.LONG,
                args("[[1000000000,2000000000],[3000000000,4000000000]]"),
                "10000000000"
        );
    }

    @Test
    @DisplayName("BOOLEAN_2D_ARRAY → BOOLEAN: anyTrue")
    void boolean2dArray_to_boolean() throws Exception {
        assertPipeline(
                "class Solution { public boolean f(boolean[][] m) { for(boolean[] r:m) for(boolean b:r) if(b) return true; return false; } }",
                "f",
                List.of(spec("m", ValueType.BOOLEAN_2D_ARRAY)),
                ValueType.BOOLEAN,
                args("[[false,false],[false,true]]"),
                "true"
        );
    }

    @Test
    @DisplayName("STRING_2D_ARRAY → STRING: [0][0] 원소")
    void string2dArray_to_string() throws Exception {
        String innerJson = mapper.writeValueAsString(new String[][]{{"hello", "world"}, {"foo", "bar"}});
        assertPipeline(
                "class Solution { public String f(String[][] m) { return m[0][0]; } }",
                "f",
                List.of(spec("m", ValueType.STRING_2D_ARRAY)),
                ValueType.STRING,
                args(innerJson),
                "hello"
        );
    }

    // ── 복합 파라미터 ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT + INT_ARRAY → INT: base + sum(arr)")
    void multiParam_int_intArray() throws Exception {
        assertPipeline(
                "class Solution { public int f(int base, int[] a) { int s=base; for(int x:a) s+=x; return s; } }",
                "f",
                List.of(spec("base", ValueType.INT), spec("a", ValueType.INT_ARRAY)),
                ValueType.INT,
                mapper.writeValueAsString(new String[]{"10", "[1,2,3]"}),
                "16"
        );
    }

    @Test
    @DisplayName("STRING + INT_ARRAY → STRING: repeat(n번) 후 합계")
    void multiParam_string_intArray() throws Exception {
        assertPipeline(
                """
                class Solution {
                    public String f(String s, int[] a) {
                        int sum = 0;
                        for (int x : a) sum += x;
                        return s + sum;
                    }
                }
                """,
                "f",
                List.of(spec("s", ValueType.STRING), spec("a", ValueType.INT_ARRAY)),
                ValueType.STRING,
                mapper.writeValueAsString(new String[]{"total=", "[1,2,3,4]"}),
                "total=10"
        );
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────────────────

    private ParameterSpec spec(String name, ValueType type) {
        return new ParameterSpec(name, type);
    }

    /** 단일 파라미터 테스트용 arguments 생성 */
    private String args(String argValue) throws Exception {
        return mapper.writeValueAsString(new String[]{argValue});
    }

    private void assertPipeline(
            String userCode, String functionName,
            List<ParameterSpec> params, ValueType returnType,
            String arguments, String expectedOutput
    ) throws Exception {
        String stdin = argumentConverter.toStdinInput(arguments, params);
        String code = codeGenerator.generateExecutableCode(
                Language.JAVA, userCode, functionName, params, returnType
        );
        String actual = runJava(code, stdin).trim();
        assertThat(actual).isEqualTo(expectedOutput.trim());
    }

    private String runJava(String code, String stdin) throws Exception {
        Path tempDir = Files.createTempDirectory("java-judge-test-");
        try {
            Path src = tempDir.resolve("Main.java");
            Files.writeString(src, code);

            // 컴파일
            Process compile = new ProcessBuilder("javac", src.toString())
                    .redirectErrorStream(true)
                    .start();
            String compileErr = new String(compile.getInputStream().readAllBytes());
            if (compile.waitFor() != 0) {
                throw new AssertionError("Compile error:\n" + compileErr + "\n\n[Generated code]\n" + code);
            }

            // 실행
            Process run = new ProcessBuilder("java", "-cp", tempDir.toString(), "Main")
                    .start();
            run.getOutputStream().write(stdin.getBytes());
            run.getOutputStream().close();

            String output = new String(run.getInputStream().readAllBytes());
            String stderr = new String(run.getErrorStream().readAllBytes());
            if (run.waitFor() != 0) {
                throw new AssertionError("Runtime error:\n" + stderr);
            }

            return output;
        } finally {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }
}
