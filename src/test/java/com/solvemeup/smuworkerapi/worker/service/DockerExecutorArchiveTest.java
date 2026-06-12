package com.solvemeup.smuworkerapi.worker.service;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DockerExecutorArchiveTest {

    @Test
    void storesLargeInputAsArchiveEntryInsteadOfCommandArgument() throws Exception {
        DockerExecutor executor = new DockerExecutor(mock(com.github.dockerjava.api.DockerClient.class));
        String largeInput = "1234567890 ".repeat(110_000);

        byte[] archive = executor.createExecutionArchive(
                "Main.java",
                "public class Main {}",
                largeInput
        );

        Map<String, String> files = readArchive(archive);
        assertThat(files.get("Main.java")).isEqualTo("public class Main {}");
        assertThat(files.get("input.txt")).isEqualTo(largeInput);
        assertThat(files.get("input.txt")).hasSizeGreaterThan(1_000_000);
    }

    private Map<String, String> readArchive(byte[] archive) throws Exception {
        Map<String, String> files = new HashMap<>();
        try (TarArchiveInputStream input = new TarArchiveInputStream(new ByteArrayInputStream(archive))) {
            TarArchiveEntry entry;
            while ((entry = input.getNextTarEntry()) != null) {
                files.put(entry.getName(), new String(input.readNBytes((int) entry.getSize()), StandardCharsets.UTF_8));
            }
        }
        return files;
    }
}
