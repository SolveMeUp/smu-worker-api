package com.solvemeup.smuworkerapi.worker.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.solvemeup.smuworkerapi.worker.enums.JudgeResult;
import com.solvemeup.smuworkerapi.worker.enums.Language;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DockerExecutor {

    private final DockerClient dockerClient;

    public ExecutionResult execute(
            Language language,
            String executableCode,
            String input,
            int testCaseNumber,
            int timeLimitMillis,
            int memoryLimitKB
    ) throws Exception {
        long startTime = System.currentTimeMillis();
        String containerId = null;

        try {
            long t0 = System.currentTimeMillis();
            containerId = createAndStartContainer(language, executableCode, input, memoryLimitKB);
            log.info("[PERF] container create+start: {}ms (testCase {})", System.currentTimeMillis() - t0, testCaseNumber);

            long t1 = System.currentTimeMillis();
            Integer exitCode = waitForContainerWithTimeout(containerId, timeLimitMillis, testCaseNumber);
            log.info("[PERF] execution wait: {}ms (testCase {})", System.currentTimeMillis() - t1, testCaseNumber);

            if (exitCode == null) {
                long executionTime = System.currentTimeMillis() - startTime;
                return new ExecutionResult(
                        JudgeResult.TLE,
                        null,
                        null,
                        (int) executionTime,
                        0
                );
            }

            long executionTime = System.currentTimeMillis() - startTime;

            long t2 = System.currentTimeMillis();
            ContainerOutput containerOutput = collectContainerOutput(containerId);
            log.info("[PERF] collect output: {}ms (testCase {})", System.currentTimeMillis() - t2, testCaseNumber);

            long t3 = System.currentTimeMillis();
            int memoryUsageKB = getMemoryUsage(containerId, testCaseNumber);
            log.info("[PERF] memory stats: {}ms (testCase {})", System.currentTimeMillis() - t3, testCaseNumber);

            return determineResult(
                    exitCode,
                    containerOutput.stdout(),
                    containerOutput.stderr(),
                    executionTime,
                    timeLimitMillis,
                    memoryUsageKB,
                    memoryLimitKB,
                    language
            );

        } finally {
            long t4 = System.currentTimeMillis();
            cleanupContainer(containerId);
            log.info("[PERF] container cleanup: {}ms", System.currentTimeMillis() - t4);
        }
    }

    private String createAndStartContainer(
            Language language,
            String executableCode,
            String input,
            int memoryLimitKB
    ) {
        DockerConfig config = getDockerConfig(language);

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withMemory((long) memoryLimitKB * 1024)
                .withMemorySwap((long) memoryLimitKB * 1024)
                .withCpuQuota(100000L)
                .withCpuPeriod(100000L)
                .withNetworkMode("none")
                .withReadonlyRootfs(false);

        CreateContainerResponse container = dockerClient.createContainerCmd(config.image)
                .withCmd(config.command)
                .withWorkingDir("/workspace")
                .withHostConfig(hostConfig)
                .withStdinOpen(true)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .exec();

        String containerId = container.getId();
        copyExecutionFiles(containerId, config.sourceFile(), executableCode, input);
        dockerClient.startContainerCmd(containerId).exec();

        return containerId;
    }

    private void copyExecutionFiles(
            String containerId,
            String sourceFile,
            String executableCode,
            String input
    ) {
        byte[] archive = createExecutionArchive(
                sourceFile,
                executableCode,
                input
        );
        dockerClient.copyArchiveToContainerCmd(containerId)
                .withRemotePath("/workspace")
                .withTarInputStream(new ByteArrayInputStream(archive))
                .exec();
    }

    byte[] createExecutionArchive(String sourceFile, String executableCode, String input) {
        return createArchive(
                new ArchiveFile(sourceFile, executableCode),
                new ArchiveFile("input.txt", input == null ? "" : input)
        );
    }

    private byte[] createArchive(ArchiveFile... files) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (TarArchiveOutputStream tar = new TarArchiveOutputStream(output)) {
                tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
                for (ArchiveFile file : files) {
                    byte[] content = file.content().getBytes(StandardCharsets.UTF_8);
                    TarArchiveEntry entry = new TarArchiveEntry(file.name());
                    entry.setSize(content.length);
                    entry.setMode(0600);
                    tar.putArchiveEntry(entry);
                    tar.write(content);
                    tar.closeArchiveEntry();
                }
                tar.finish();
            }
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create container input archive", e);
        }
    }

    private Integer waitForContainerWithTimeout(
            String containerId,
            int timeLimitMillis,
            int testCaseNumber
    ) throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Future<Integer> future = executor.submit(() -> {
                WaitContainerResultCallback callback = new WaitContainerResultCallback();
                dockerClient.waitContainerCmd(containerId).exec(callback);
                return callback.awaitStatusCode();
            });

            try {
                return future.get(timeLimitMillis + 2000, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                log.warn("Test case {} exceeded time limit", testCaseNumber);
                future.cancel(true);
                return null;
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private ContainerOutput collectContainerOutput(String containerId) throws InterruptedException {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        dockerClient.logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)
                .exec(new LogContainerResultCallback() {
                    @Override
                    public void onNext(Frame frame) {
                        if (frame.getStreamType() == StreamType.STDOUT) {
                            stdout.writeBytes(frame.getPayload());
                        } else if (frame.getStreamType() == StreamType.STDERR) {
                            stderr.writeBytes(frame.getPayload());
                        }
                    }
                }).awaitCompletion();

        String stdoutStr = stdout.toString(StandardCharsets.UTF_8);
        String stderrStr = stderr.toString(StandardCharsets.UTF_8);

        return new ContainerOutput(stdoutStr, stderrStr);
    }

    private int getMemoryUsage(String containerId, int testCaseNumber) {
        try {
            CompletableFuture<Integer> memoryFuture = new CompletableFuture<>();

            dockerClient.statsCmd(containerId)
                    .withNoStream(true)
                    .exec(new ResultCallback.Adapter<Statistics>() {
                        @Override
                        public void onNext(Statistics stats) {
                            if (stats.getMemoryStats() != null && stats.getMemoryStats().getUsage() != null) {
                                int memoryKB = (int) (stats.getMemoryStats().getUsage() / 1024);
                                memoryFuture.complete(memoryKB);
                            } else {
                                memoryFuture.complete(0);
                            }
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            memoryFuture.complete(0);
                        }
                    });

            return memoryFuture.get(3, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.warn("Failed to get memory stats for test case {}: {}", testCaseNumber, e.getMessage());
            return 0;
        }
    }

    private ExecutionResult determineResult(
            Integer exitCode,
            String output,
            String error,
            long executionTime,
            int timeLimitMillis,
            int memoryUsageKB,
            int memoryLimitKB,
            Language language
    ) {
        if (executionTime > timeLimitMillis) {
            return new ExecutionResult(
                    JudgeResult.TLE,
                    output,
                    error,
                    (int) executionTime,
                    memoryUsageKB
            );
        }

        if (memoryUsageKB > memoryLimitKB) {
            return new ExecutionResult(
                    JudgeResult.MLE,
                    output,
                    error,
                    (int) executionTime,
                    memoryUsageKB
            );
        }

        if (exitCode == null || exitCode != 0) {
            JudgeResult result = determineErrorType(error, language);
            return new ExecutionResult(
                    result,
                    output,
                    error,
                    (int) executionTime,
                    memoryUsageKB
            );
        }

        return new ExecutionResult(
                JudgeResult.AC,
                output,
                error,
                (int) executionTime,
                memoryUsageKB
        );
    }

    private void cleanupContainer(String containerId) {
        if (containerId == null) {
            return;
        }

        try {
            dockerClient.removeContainerCmd(containerId)
                    .withForce(true)
                    .exec();
            log.debug("Container removed: {}", containerId.substring(0, 12));
        } catch (Exception e) {
            log.error("Failed to remove container {}: {}", containerId.substring(0, 12), e.getMessage());
        }
    }

    private JudgeResult determineErrorType(String error, Language language) {
        if (error == null) {
            return JudgeResult.RE;
        }

        String lowerError = error.toLowerCase();

        return switch (language) {
            case JAVA -> {
                if (lowerError.contains("error:") &&
                        (lowerError.contains(".java:") || lowerError.contains("cannot find symbol"))) {
                    yield JudgeResult.CE;
                }
                yield JudgeResult.RE;
            }
            case CPP -> {
                if (lowerError.contains("error:") &&
                        (lowerError.contains("compilation terminated") || lowerError.contains("expected"))) {
                    yield JudgeResult.CE;
                }
                yield JudgeResult.RE;
            }
            case PYTHON -> {
                if (lowerError.contains("syntaxerror") || lowerError.contains("indentationerror")) {
                    yield JudgeResult.CE;
                }
                yield JudgeResult.RE;
            }
        };
    }

    private DockerConfig getDockerConfig(Language language) {
        return switch (language) {
            case JAVA -> new DockerConfig(
                    "eclipse-temurin:17-jdk",
                    "Main.java",
                    new String[]{"sh", "-c",
                            "javac Main.java && java Main < input.txt"}
            );
            case PYTHON -> new DockerConfig(
                    "python:3.11-slim",
                    "solution.py",
                    new String[]{"sh", "-c",
                            "python solution.py < input.txt"}
            );
            case CPP -> new DockerConfig(
                    "gcc:13",
                    "solution.cpp",
                    new String[]{"sh", "-c",
                            "g++ -o solution solution.cpp && ./solution < input.txt"}
            );
        };
    }

    /**
     * Runs every test case in a single container/process (LeetCode-style batch).
     * Compiles once, feeds all case inputs through one stdin stream, and parses the
     * boundary-delimited per-case output produced by the batch driver.
     */
    public BatchExecutionResult executeBatch(
            Language language,
            String executableCode,
            List<String> perCaseStdin,
            int timeLimitMillis,
            int memoryLimitKB
    ) {
        String boundary = "SMUCASE" + UUID.randomUUID().toString().replace("-", "");
        int caseCount = perCaseStdin.size();
        String stdin = buildBatchStdin(perCaseStdin);

        String containerId = null;
        try {
            long t0 = System.currentTimeMillis();
            containerId = createAndStartBatchContainer(
                    language, executableCode, stdin, boundary, timeLimitMillis, memoryLimitKB);

            long backstopMillis = Math.min((long) timeLimitMillis * caseCount + 5000L, 300000L);
            Integer exitCode = waitForContainerBackstop(containerId, backstopMillis);
            boolean timedOut = exitCode == null;
            log.info("[PERF] batch run: {}ms ({} cases, exit={}, timedOut={})",
                    System.currentTimeMillis() - t0, caseCount, exitCode, timedOut);

            ContainerOutput output = collectContainerOutput(containerId);
            boolean oomKilled = isOomKilled(containerId);

            return parseBatchOutput(
                    boundary, caseCount, output.stdout(), output.stderr(),
                    timedOut, oomKilled, timeLimitMillis, language);

        } catch (Exception e) {
            log.error("Batch execution failed", e);
            List<BatchCaseResult> failed = new ArrayList<>();
            for (int i = 0; i < caseCount; i++) {
                failed.add(new BatchCaseResult(JudgeResult.SYSTEM_ERROR, null, 0, 0));
            }
            return new BatchExecutionResult(failed, e.getMessage());
        } finally {
            cleanupContainer(containerId);
        }
    }

    String buildBatchStdin(List<String> perCaseStdin) {
        StringBuilder sb = new StringBuilder();
        sb.append(perCaseStdin.size()).append('\n');
        for (String caseInput : perCaseStdin) {
            String value = caseInput == null ? "" : caseInput;
            sb.append(value);
            if (!value.endsWith("\n")) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private String createAndStartBatchContainer(
            Language language,
            String executableCode,
            String stdin,
            String boundary,
            int timeLimitMillis,
            int memoryLimitKB
    ) {
        DockerConfig config = getBatchDockerConfig(language);

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withMemory((long) memoryLimitKB * 1024)
                .withMemorySwap((long) memoryLimitKB * 1024)
                .withCpuQuota(100000L)
                .withCpuPeriod(100000L)
                .withNetworkMode("none")
                .withReadonlyRootfs(false);

        CreateContainerResponse container = dockerClient.createContainerCmd(config.image())
                .withCmd(config.command())
                .withWorkingDir("/workspace")
                .withEnv("SMU_BOUNDARY=" + boundary, "TIME_LIMIT_MS=" + timeLimitMillis)
                .withHostConfig(hostConfig)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .exec();

        String containerId = container.getId();
        copyExecutionFiles(containerId, config.sourceFile(), executableCode, stdin);
        dockerClient.startContainerCmd(containerId).exec();

        return containerId;
    }

    private Integer waitForContainerBackstop(String containerId, long timeoutMillis)
            throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Integer> future = executor.submit(() -> {
                WaitContainerResultCallback callback = new WaitContainerResultCallback();
                dockerClient.waitContainerCmd(containerId).exec(callback);
                return callback.awaitStatusCode();
            });
            try {
                return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                return null;
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean isOomKilled(String containerId) {
        try {
            Boolean oom = dockerClient.inspectContainerCmd(containerId).exec()
                    .getState().getOOMKilled();
            return Boolean.TRUE.equals(oom);
        } catch (Exception e) {
            return false;
        }
    }

    BatchExecutionResult parseBatchOutput(
            String boundary,
            int caseCount,
            String stdout,
            String stderr,
            boolean timedOut,
            boolean oomKilled,
            int timeLimitMillis,
            Language language
    ) {
        String[] lines = stdout.split("\n", -1);
        java.util.Map<Integer, BatchCaseResult> parsed = new java.util.HashMap<>();

        Integer currentCase = null;
        String currentStatus = null;
        int currentMillis = 0;
        StringBuilder currentOutput = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith(boundary + " ")) {
                if (currentCase != null) {
                    parsed.put(currentCase, toCaseResult(currentStatus, currentMillis, currentOutput.toString()));
                }
                String[] parts = line.split(" ");
                currentCase = parseIntSafe(parts.length > 1 ? parts[1] : null, -1);
                currentStatus = parts.length > 2 ? parts[2] : "RE";
                currentMillis = parseIntSafe(parts.length > 3 ? parts[3] : null, 0);
                currentOutput.setLength(0);
            } else if (currentCase != null) {
                if (currentOutput.length() > 0) {
                    currentOutput.append('\n');
                }
                currentOutput.append(line);
            }
        }
        if (currentCase != null) {
            parsed.put(currentCase, toCaseResult(currentStatus, currentMillis, currentOutput.toString()));
        }

        JudgeResult deathVerdict = resolveDeathVerdict(
                parsed.isEmpty(), oomKilled, timedOut, stderr, language);

        List<BatchCaseResult> results = new ArrayList<>(caseCount);
        for (int i = 0; i < caseCount; i++) {
            BatchCaseResult result = parsed.get(i);
            results.add(result != null
                    ? result
                    : new BatchCaseResult(deathVerdict, null,
                        deathVerdict == JudgeResult.TLE ? timeLimitMillis : 0, 0));
        }
        return new BatchExecutionResult(results, stderr);
    }

    private BatchCaseResult toCaseResult(String status, int millis, String caseOutput) {
        return switch (status) {
            case "OK" -> new BatchCaseResult(JudgeResult.AC, stripTrailingNewline(caseOutput), millis, 0);
            case "TLE" -> new BatchCaseResult(JudgeResult.TLE, null, millis, 0);
            default -> new BatchCaseResult(JudgeResult.RE, stripTrailingNewline(caseOutput), millis, 0);
        };
    }

    private JudgeResult resolveDeathVerdict(
            boolean noCasesParsed, boolean oomKilled, boolean timedOut, String stderr, Language language) {
        if (oomKilled) {
            return JudgeResult.MLE;
        }
        if (timedOut) {
            return JudgeResult.TLE;
        }
        if (noCasesParsed) {
            return determineErrorType(stderr, language);
        }
        return JudgeResult.RE;
    }

    private String stripTrailingNewline(String value) {
        if (value == null) {
            return null;
        }
        int end = value.length();
        while (end > 0 && (value.charAt(end - 1) == '\n' || value.charAt(end - 1) == '\r')) {
            end--;
        }
        return value.substring(0, end);
    }

    private int parseIntSafe(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private DockerConfig getBatchDockerConfig(Language language) {
        return switch (language) {
            case JAVA -> new DockerConfig(
                    "eclipse-temurin:17-jdk",
                    "Main.java",
                    new String[]{"sh", "-c",
                            "javac Main.java && java -XX:TieredStopAtLevel=1 -XX:+UseSerialGC Main < input.txt"}
            );
            case PYTHON -> new DockerConfig(
                    "python:3.11-slim",
                    "solution.py",
                    new String[]{"sh", "-c",
                            "python solution.py < input.txt"}
            );
            case CPP -> new DockerConfig(
                    "gcc:13",
                    "solution.cpp",
                    new String[]{"sh", "-c",
                            "g++ -O2 -std=c++17 -o solution solution.cpp && ./solution < input.txt"}
            );
        };
    }

    public record BatchExecutionResult(
            List<BatchCaseResult> cases,
            String stderr
    ) {}

    public record BatchCaseResult(
            JudgeResult status,
            String output,
            int executionTimeMillis,
            int memoryUsageKB
    ) {}

    public record ExecutionResult(
            JudgeResult status,
            String output,
            String error,
            int executionTimeMillis,
            int memoryUsageKB
    ) {}

    private record DockerConfig(String image, String sourceFile, String[] command) {}

    private record ArchiveFile(String name, String content) {}

    private record ContainerOutput(String stdout, String stderr) {}

}
