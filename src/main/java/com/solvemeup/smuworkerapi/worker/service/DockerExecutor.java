package com.solvemeup.smuworkerapi.worker.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.solvemeup.smuworkerapi.worker.config.DockerConfig;
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
}