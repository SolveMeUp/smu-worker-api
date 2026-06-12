package com.solvemeup.smuworkerapi.worker.service;

import com.solvemeup.smuworkerapi.worker.dto.ParameterSpec;
import com.solvemeup.smuworkerapi.worker.enums.Language;
import com.solvemeup.smuworkerapi.worker.enums.ValueType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Submit 플로우 정확성 검증
 *
 * SubmissionService는 파일시스템의 TestCase.input(stdin 포맷)을 그대로 실행에 사용합니다.
 * arguments → toStdinInput() 변환 없이 stdin을 직접 주입합니다.
 *
 *   DB TestCase.input (stdin 포맷)
 *              ↓
 *   generateExecutableCode()
 *              ↓
 *        javac + java 실행 → output 검증
 */
class CodeGeneratorJavaStdinTest {

    private CodeGenerator codeGenerator;

    @BeforeEach
    void setUp() {
        codeGenerator = new CodeGenerator();
    }

    // ── 스칼라 타입 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT stdin → INT 반환")
    void int_stdin() throws Exception {
        assertPipeline(
                "class Solution { public int f(int n) { return n * 2; } }",
                "f",
                List.of(spec("n", ValueType.INT)),
                ValueType.INT,
                "5",
                "10"
        );
    }

    @Test
    @DisplayName("LONG stdin → LONG 반환")
    void long_stdin() throws Exception {
        assertPipeline(
                "class Solution { public long f(long n) { return n + 1L; } }",
                "f",
                List.of(spec("n", ValueType.LONG)),
                ValueType.LONG,
                "9999999999",
                "10000000000"
        );
    }

    @Test
    @DisplayName("BOOLEAN stdin → BOOLEAN 반환")
    void boolean_stdin() throws Exception {
        assertPipeline(
                "class Solution { public boolean f(boolean b) { return !b; } }",
                "f",
                List.of(spec("b", ValueType.BOOLEAN)),
                ValueType.BOOLEAN,
                "true",
                "false"
        );
    }

    @Test
    @DisplayName("STRING stdin → STRING 반환")
    void string_stdin() throws Exception {
        assertPipeline(
                "class Solution { public String f(String s) { return s.toUpperCase(); } }",
                "f",
                List.of(spec("s", ValueType.STRING)),
                ValueType.STRING,
                "hello world",
                "HELLO WORLD"
        );
    }

    // ── 1D 배열 타입 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT_ARRAY stdin → INT 반환 (합계)")
    void intArray_stdin_to_int() throws Exception {
        assertPipeline(
                "class Solution { public int f(int[] a) { int s=0; for(int x:a) s+=x; return s; } }",
                "f",
                List.of(spec("a", ValueType.INT_ARRAY)),
                ValueType.INT,
                "1 2 3 4 5",
                "15"
        );
    }

    @Test
    @DisplayName("INT_ARRAY stdin → INT_ARRAY 반환 (identity)")
    void intArray_stdin_to_intArray() throws Exception {
        assertPipeline(
                "class Solution { public int[] f(int[] a) { return a; } }",
                "f",
                List.of(spec("a", ValueType.INT_ARRAY)),
                ValueType.INT_ARRAY,
                "1 2 3",
                "1 2 3"
        );
    }

    @Test
    @DisplayName("LONG_ARRAY stdin → LONG 반환 (합계)")
    void longArray_stdin_to_long() throws Exception {
        assertPipeline(
                "class Solution { public long f(long[] a) { long s=0; for(long x:a) s+=x; return s; } }",
                "f",
                List.of(spec("a", ValueType.LONG_ARRAY)),
                ValueType.LONG,
                "1000000000 2000000000 3000000000",
                "6000000000"
        );
    }

    @Test
    @DisplayName("BOOLEAN_ARRAY stdin → BOOLEAN_ARRAY 반환 (identity)")
    void booleanArray_stdin_to_booleanArray() throws Exception {
        assertPipeline(
                "class Solution { public boolean[] f(boolean[] a) { return a; } }",
                "f",
                List.of(spec("a", ValueType.BOOLEAN_ARRAY)),
                ValueType.BOOLEAN_ARRAY,
                "true false true",
                "true false true"
        );
    }

    @Test
    @DisplayName("STRING_ARRAY stdin → STRING_ARRAY 반환 (identity)")
    void stringArray_stdin_to_stringArray() throws Exception {
        // STRING_ARRAY stdin 포맷: 첫 줄에 개수, 이후 한 줄씩
        assertPipeline(
                "class Solution { public String[] f(String[] a) { return a; } }",
                "f",
                List.of(spec("a", ValueType.STRING_ARRAY)),
                ValueType.STRING_ARRAY,
                "3\napple\nbanana\ncherry",
                "3\napple\nbanana\ncherry"
        );
    }

    // ── 2D 배열 타입 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT_2D_ARRAY stdin → INT 반환 (전체 합)")
    void int2dArray_stdin_to_int() throws Exception {
        // INT_2D_ARRAY stdin 포맷: 첫 줄에 "rows cols", 이후 행별 공백 구분
        assertPipeline(
                "class Solution { public int f(int[][] m) { int s=0; for(int[] r:m) for(int x:r) s+=x; return s; } }",
                "f",
                List.of(spec("m", ValueType.INT_2D_ARRAY)),
                ValueType.INT,
                "3 2\n1 2\n3 4\n5 6",
                "21"
        );
    }

    @Test
    @DisplayName("INT_2D_ARRAY stdin → INT_2D_ARRAY 반환 (identity)")
    void int2dArray_stdin_to_int2dArray() throws Exception {
        assertPipeline(
                "class Solution { public int[][] f(int[][] m) { return m; } }",
                "f",
                List.of(spec("m", ValueType.INT_2D_ARRAY)),
                ValueType.INT_2D_ARRAY,
                "2 3\n1 2 3\n4 5 6",
                "2 3\n1 2 3\n4 5 6"
        );
    }

    @Test
    @DisplayName("LONG_2D_ARRAY stdin → LONG 반환 (전체 합)")
    void long2dArray_stdin_to_long() throws Exception {
        assertPipeline(
                "class Solution { public long f(long[][] m) { long s=0; for(long[] r:m) for(long x:r) s+=x; return s; } }",
                "f",
                List.of(spec("m", ValueType.LONG_2D_ARRAY)),
                ValueType.LONG,
                "2 2\n1000000000 2000000000\n3000000000 4000000000",
                "10000000000"
        );
    }

    @Test
    @DisplayName("BOOLEAN_2D_ARRAY stdin → BOOLEAN 반환 (anyTrue)")
    void boolean2dArray_stdin_to_boolean() throws Exception {
        assertPipeline(
                "class Solution { public boolean f(boolean[][] m) { for(boolean[] r:m) for(boolean b:r) if(b) return true; return false; } }",
                "f",
                List.of(spec("m", ValueType.BOOLEAN_2D_ARRAY)),
                ValueType.BOOLEAN,
                "2 2\nfalse false\nfalse true",
                "true"
        );
    }

    @Test
    @DisplayName("STRING_2D_ARRAY stdin → STRING 반환 ([0][0] 원소)")
    void string2dArray_stdin_to_string() throws Exception {
        // STRING_2D_ARRAY stdin 포맷: 첫 줄에 "rows cols", 이후 행별 공백 구분
        assertPipeline(
                "class Solution { public String f(String[][] m) { return m[0][0]; } }",
                "f",
                List.of(spec("m", ValueType.STRING_2D_ARRAY)),
                ValueType.STRING,
                "2 2\nhello world\nfoo bar",
                "hello"
        );
    }

    // ── 복합 파라미터 (Submit에서 자주 나오는 패턴) ────────────────────────────────

    @Test
    @DisplayName("복합 파라미터: INT_ARRAY + INT stdin")
    void multiParam_intArray_int_stdin() throws Exception {
        assertPipeline(
                "class Solution { public int[] f(int[] nums, int target) { return new int[]{nums[0]+target, nums[1]+target}; } }",
                "f",
                List.of(spec("nums", ValueType.INT_ARRAY), spec("target", ValueType.INT)),
                ValueType.INT_ARRAY,
                "2 7\n9",
                "11 16"
        );
    }

    @Test
    @DisplayName("복합 파라미터: INT_2D_ARRAY + INT_ARRAY stdin")
    void multiParam_int2dArray_intArray_stdin() throws Exception {
        assertPipeline(
                """
                class Solution {
                    public int f(int[][] m, int[] targets) {
                        int sum = 0;
                        for (int t : targets) sum += m[0][t];
                        return sum;
                    }
                }
                """,
                "f",
                List.of(spec("m", ValueType.INT_2D_ARRAY), spec("targets", ValueType.INT_ARRAY)),
                ValueType.INT,
                "2 3\n10 20 30\n40 50 60\n0 1 2",
                "60"
        );
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────────────────

    private ParameterSpec spec(String name, ValueType type) {
        return new ParameterSpec(name, type);
    }

    private void assertPipeline(
            String userCode, String functionName,
            List<ParameterSpec> params, ValueType returnType,
            String stdin, String expectedOutput
    ) throws Exception {
        String code = codeGenerator.generateExecutableCode(
                Language.JAVA, userCode, functionName, params, returnType
        );
        String actual = runJava(code, stdin).trim();
        assertThat(actual).isEqualTo(expectedOutput.trim());
    }

    private String runJava(String code, String stdin) throws Exception {
        Path tempDir = Files.createTempDirectory("java-submit-test-");
        try {
            Path src = tempDir.resolve("Main.java");
            Files.writeString(src, code);

            Process compile = new ProcessBuilder("javac", src.toString())
                    .redirectErrorStream(true)
                    .start();
            String compileErr = new String(compile.getInputStream().readAllBytes());
            if (compile.waitFor() != 0) {
                throw new AssertionError("Compile error:\n" + compileErr + "\n\n[Generated code]\n" + code);
            }

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
