package com.solvemeup.smuworkerapi.worker.service;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

/**
 * Scanner vs BufferedReader 성능 비교 테스트
 *
 * 기존 CodeGenerator(Scanner) 코드를 수정하지 않고,
 * 테스트 내부에 BufferedReader 버전의 코드 생성 로직을 별도로 작성하여 비교합니다.
 *
 * 측정 방식:
 *   - 컴파일(javac)은 1회만 수행
 *   - 실행(java)을 ROUNDS회 반복하여 평균 실행 시간 측정
 *   - JVM 워밍업을 위해 WARMUP회 선실행
 */
class CodeGeneratorPerformanceTest {

    private static final int WARMUP = 3;
    private static final int ROUNDS = 10;

    // ── INT_ARRAY: N = 1,000 ─────────────────────────────────────────────────

    @Test
    void benchmark_intArray_small_1K() throws Exception {
        runBenchmark("INT_ARRAY", 1_000, generateIntArrayStdin(1_000));
    }

    // ── INT_ARRAY: N = 100,000 ───────────────────────────────────────────────

    @Test
    void benchmark_intArray_medium_100K() throws Exception {
        runBenchmark("INT_ARRAY", 100_000, generateIntArrayStdin(100_000));
    }

    // ── INT_ARRAY: N = 1,000,000 ─────────────────────────────────────────────

    @Test
    void benchmark_intArray_large_1M() throws Exception {
        runBenchmark("INT_ARRAY", 1_000_000, generateIntArrayStdin(1_000_000));
    }

    // ── INT_2D_ARRAY: 500 × 500 = 250,000개 ─────────────────────────────────

    @Test
    void benchmark_int2dArray_medium_500x500() throws Exception {
        runBenchmark2D("INT_2D_ARRAY", 500, 500, generate2DArrayStdin(500, 500));
    }

    // ── 벤치마크 실행 ─────────────────────────────────────────────────────────

    private void runBenchmark(String label, int n, String stdin) throws Exception {
        String userCode = "class Solution { public long f(int[] arr) { long s=0; for(int x:arr) s+=x; return s; } }";

        String scannerCode = generateScannerIntArrayCode(userCode);
        String brCode = generateBrIntArrayCode(userCode);

        printResult(label, n, scannerCode, brCode, stdin);
    }

    private void runBenchmark2D(String label, int rows, int cols, String stdin) throws Exception {
        String userCode = "class Solution { public long f(int[][] m) { long s=0; for(int[] r:m) for(int x:r) s+=x; return s; } }";

        String scannerCode = generateScannerInt2dArrayCode(userCode);
        String brCode = generateBrInt2dArrayCode(userCode);

        printResult(label, rows * cols, scannerCode, brCode, stdin);
    }

    private void printResult(String label, int n, String scannerCode, String brCode, String stdin)
            throws Exception {

        Path scannerDir = compile(scannerCode);
        Path brDir = compile(brCode);

        try {
            System.out.printf("%n=== %s (N=%,d) ===%n", label, n);

            // Warmup
            for (int i = 0; i < WARMUP; i++) {
                runOnce(scannerDir, stdin);
                runOnce(brDir, stdin);
            }

            // Scanner 측정
            long scannerTotal = 0;
            for (int i = 0; i < ROUNDS; i++) {
                scannerTotal += runOnce(scannerDir, stdin);
            }

            // BufferedReader 측정
            long brTotal = 0;
            for (int i = 0; i < ROUNDS; i++) {
                brTotal += runOnce(brDir, stdin);
            }

            double scannerAvg = scannerTotal / (double) ROUNDS;
            double brAvg = brTotal / (double) ROUNDS;
            double ratio = scannerAvg / brAvg;

            System.out.printf("Scanner        avg: %6.1f ms  (total: %,d ms)%n", scannerAvg, scannerTotal);
            System.out.printf("BufferedReader avg: %6.1f ms  (total: %,d ms)%n", brAvg, brTotal);
            if (ratio >= 1.0) {
                System.out.printf("결과: BufferedReader가 %.2fx 빠름%n", ratio);
            } else {
                System.out.printf("결과: Scanner가 %.2fx 빠름%n", 1.0 / ratio);
            }
        } finally {
            deleteDir(scannerDir);
            deleteDir(brDir);
        }
    }

    // ── 컴파일 / 실행 ─────────────────────────────────────────────────────────

    private Path compile(String code) throws Exception {
        Path dir = Files.createTempDirectory("perf-test-");
        Path src = dir.resolve("Main.java");
        Files.writeString(src, code);

        Process compile = new ProcessBuilder("javac", src.toString())
                .redirectErrorStream(true)
                .start();
        String err = new String(compile.getInputStream().readAllBytes());
        if (compile.waitFor() != 0) {
            deleteDir(dir);
            throw new AssertionError("Compile error:\n" + err + "\n\n" + code);
        }
        return dir;
    }

    /** 실행 후 실제 소요 시간(ms) 반환 */
    private long runOnce(Path classDir, String stdin) throws Exception {
        long start = System.currentTimeMillis();

        Process run = new ProcessBuilder("java", "-cp", classDir.toString(), "Main")
                .start();
        run.getOutputStream().write(stdin.getBytes());
        run.getOutputStream().close();
        run.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
        run.getErrorStream().transferTo(java.io.OutputStream.nullOutputStream());
        run.waitFor();

        return System.currentTimeMillis() - start;
    }

    private void deleteDir(Path dir) throws Exception {
        Files.walk(dir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }

    // ── stdin 생성 ────────────────────────────────────────────────────────────

    private String generateIntArrayStdin(int n) {
        StringJoiner sj = new StringJoiner(" ");
        for (int i = 0; i < n; i++) sj.add(String.valueOf(i % 1000));
        return sj.toString();
    }

    private String generate2DArrayStdin(int rows, int cols) {
        StringBuilder sb = new StringBuilder();
        sb.append(rows).append(' ').append(cols);
        for (int i = 0; i < rows; i++) {
            sb.append('\n');
            StringJoiner sj = new StringJoiner(" ");
            for (int j = 0; j < cols; j++) sj.add(String.valueOf((i * cols + j) % 1000));
            sb.append(sj);
        }
        return sb.toString();
    }

    // ── Scanner 버전 코드 생성 (전환 전 코드 재현) ──────────────────────────────

    private String generateScannerIntArrayCode(String userCode) {
        return """
                import java.util.*;
                import java.io.*;

                """ + userCode + """


                public class Main {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        Solution solution = new Solution();

                        String line_arr = sc.nextLine().trim();
                        if (line_arr.isEmpty() && sc.hasNextLine()) line_arr = sc.nextLine().trim();
                        String[] tokens_arr = line_arr.split("\\\\s+");
                        int[] arr = new int[tokens_arr.length];
                        for (int i = 0; i < tokens_arr.length; i++) {
                            arr[i] = Integer.parseInt(tokens_arr[i]);
                        }

                        long result = solution.f(arr);
                        System.out.println(result);
                        sc.close();
                    }
                }
                """;
    }

    private String generateScannerInt2dArrayCode(String userCode) {
        return """
                import java.util.*;
                import java.io.*;

                """ + userCode + """


                public class Main {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        Solution solution = new Solution();

                        int rows_m = sc.nextInt();
                        int cols_m = sc.nextInt();
                        int[][] m = new int[rows_m][cols_m];
                        for (int i = 0; i < rows_m; i++) {
                            for (int j = 0; j < cols_m; j++) {
                                m[i][j] = sc.nextInt();
                            }
                        }

                        long result = solution.f(m);
                        System.out.println(result);
                        sc.close();
                    }
                }
                """;
    }

    // ── BufferedReader 버전 코드 생성 (전환 후 코드) ─────────────────────────────
    private String generateBrIntArrayCode(String userCode) {
        return """
                import java.util.*;
                import java.io.*;

                """ + userCode + """


                public class Main {
                    public static void main(String[] args) throws Exception {
                        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                        Solution solution = new Solution();

                        String[] tokens_arr = br.readLine().trim().split("\\\\s+");
                        int[] arr = new int[tokens_arr.length];
                        for (int i = 0; i < tokens_arr.length; i++) {
                            arr[i] = Integer.parseInt(tokens_arr[i]);
                        }

                        long result = solution.f(arr);
                        System.out.println(result);
                    }
                }
                """;
    }

    /**
     * INT_2D_ARRAY 파라미터 → LONG 반환 BufferedReader 버전
     */
    private String generateBrInt2dArrayCode(String userCode) {
        return """
                import java.util.*;
                import java.io.*;

                """ + userCode + """


                public class Main {
                    public static void main(String[] args) throws Exception {
                        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                        Solution solution = new Solution();

                        String[] dims_m = br.readLine().trim().split("\\\\s+");
                        int rows_m = Integer.parseInt(dims_m[0]);
                        int cols_m = Integer.parseInt(dims_m[1]);
                        int[][] m = new int[rows_m][cols_m];
                        for (int i = 0; i < rows_m; i++) {
                            String[] row_m = br.readLine().trim().split("\\\\s+");
                            for (int j = 0; j < cols_m; j++) {
                                m[i][j] = Integer.parseInt(row_m[j]);
                            }
                        }

                        long result = solution.f(m);
                        System.out.println(result);
                    }
                }
                """;
    }
}
