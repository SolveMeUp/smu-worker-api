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
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
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
            int memoryLimitMB
    ) throws Exception {
        long startTime = System.currentTimeMillis();
        String containerId = null;

        try {
            containerId = createAndStartContainer(language, executableCode, input, memoryLimitMB);
            log.debug("Container created: {} for test case {}", containerId.substring(0, 12), testCaseNumber);

            Integer exitCode = waitForContainerWithTimeout(containerId, timeLimitMillis, testCaseNumber);

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

            ContainerOutput containerOutput = collectContainerOutput(containerId);

            int memoryUsageKB = getMemoryUsage(containerId, testCaseNumber);

            return determineResult(
                    exitCode,
                    containerOutput.stdout(),
                    containerOutput.stderr(),
                    executionTime,
                    timeLimitMillis,
                    memoryUsageKB,
                    memoryLimitMB,
                    language
            );

        } finally {
            cleanupContainer(containerId);
        }
    }

    private String createAndStartContainer(
            Language language,
            String executableCode,
            String input,
            int memoryLimitMB
    ) {
        DockerConfig config = getDockerConfig(language, executableCode, input);

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withMemory((long) memoryLimitMB * 1024 * 1024)
                .withMemorySwap((long) memoryLimitMB * 1024 * 1024)
                .withCpuQuota(100000L)
                .withCpuPeriod(100000L)
                .withNetworkMode("none")
                .withReadonlyRootfs(false);

        CreateContainerResponse container = dockerClient.createContainerCmd(config.image)
                .withCmd(config.command)
                .withHostConfig(hostConfig)
                .withStdinOpen(true)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .exec();

        String containerId = container.getId();
        dockerClient.startContainerCmd(containerId).exec();

        return containerId;
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
                    .exec(new ResultCallback.Adapter<com.github.dockerjava.api.model.Statistics>() {
                        @Override
                        public void onNext(com.github.dockerjava.api.model.Statistics stats) {
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
            int memoryLimitMB,
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

        if (memoryUsageKB > (long) memoryLimitMB * 1024) {
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

    private DockerConfig getDockerConfig(Language language, String code, String input) {
        String escapedCode = escapeCode(code);
        String escapedInput = escapeCode(input != null ? input : "");

        return switch (language) {
            case JAVA -> new DockerConfig(
                    "eclipse-temurin:17-jdk",
                    new String[]{"sh", "-c",
                            "echo '" + escapedCode + "' > Main.java && " +
                                    "javac Main.java 2>&1 && " +
                                    "echo '" + escapedInput + "' | java Main 2>&1"}
            );
            case PYTHON -> new DockerConfig(
                    "python:3.11-slim",
                    new String[]{"sh", "-c",
                            "echo '" + escapedCode + "' > solution.py && " +
                                    "echo '" + escapedInput + "' | python solution.py 2>&1"}
            );
            case CPP -> new DockerConfig(
                    "gcc:13",
                    new String[]{"sh", "-c",
                            "echo '" + escapedCode + "' > solution.cpp && " +
                                    "g++ -o solution solution.cpp 2>&1 && " +
                                    "echo '" + escapedInput + "' | ./solution 2>&1"}
            );
        };
    }

    private String escapeCode(String code) {
        return code
                .replace("\\", "\\\\")
                .replace("'", "'\\''")
                .replace("$", "\\$")
                .replace("`", "\\`");
    }

    public record ExecutionResult(
            JudgeResult status,
            String output,
            String error,
            int executionTimeMillis,
            int memoryUsageKB
    ) {}

    private record DockerConfig(String image, String[] command) {}

    private record ContainerOutput(String stdout, String stderr) {}

}