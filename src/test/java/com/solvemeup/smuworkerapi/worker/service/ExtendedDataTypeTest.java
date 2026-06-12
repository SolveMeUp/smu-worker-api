package com.solvemeup.smuworkerapi.worker.service;

import com.solvemeup.smuworkerapi.worker.dto.ParameterSpec;
import com.solvemeup.smuworkerapi.worker.enums.Language;
import com.solvemeup.smuworkerapi.worker.enums.ValueType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExtendedDataTypeTest {

    private final ArgumentConverter argumentConverter = new ArgumentConverter();
    private final CodeGenerator codeGenerator = new CodeGenerator();

    @Test
    void convertsNewCoreDataTypesToStdinAndBack() {
        List<ParameterSpec> parameters = List.of(
                new ParameterSpec("letter", ValueType.CHAR),
                new ParameterSpec("ratio", ValueType.DOUBLE),
                new ParameterSpec("matrix", ValueType.DOUBLE_2D_ARRAY),
                new ParameterSpec("cube", ValueType.CHAR_3D_ARRAY)
        );
        String arguments = "[\"x\",1.5,[[1.0,2.0],[3.0,4.0]],[[[\"a\",\"b\"]],[['c','d']]]]"
                .replace('\'', '"');

        String stdin = argumentConverter.toStdinInput(arguments, parameters);

        assertThat(argumentConverter.stdinToArgumentsJson(stdin, parameters))
                .isEqualTo("[\"x\",1.5,[[1.0,2.0],[3.0,4.0]],[[[\"a\",\"b\"]],[[\"c\",\"d\"]]]]");
    }

    @Test
    void pythonGeneratorHandlesDoubleAndThreeDimensionalArray() throws Exception {
        String code = codeGenerator.generateExecutableCode(
                Language.PYTHON,
                "class Solution:\n    def solve(self, cube, factor):\n        return cube[0][0][0] * factor",
                "solve",
                List.of(
                        new ParameterSpec("cube", ValueType.DOUBLE_3D_ARRAY),
                        new ParameterSpec("factor", ValueType.DOUBLE)
                ),
                ValueType.DOUBLE
        );

        assertThat(run(List.of("python3", "solution.py"), "1 1 2\n1.5 2.5\n2", code, "solution.py"))
                .isEqualTo("3.0");
    }

    @Test
    void cppGeneratorHandlesCharAndJsonArrayOutput() throws Exception {
        String code = codeGenerator.generateExecutableCode(
                Language.CPP,
                "class Solution { public: vector<char> solve(vector<char> values) { return values; } };",
                "solve",
                List.of(new ParameterSpec("values", ValueType.CHAR_ARRAY)),
                ValueType.CHAR_ARRAY
        );

        Path directory = Files.createTempDirectory("cpp-generator-test-");
        try {
            Path source = directory.resolve("solution.cpp");
            Files.writeString(source, code);
            Path executable = directory.resolve("solution");
            Process compile = new ProcessBuilder("g++", "-std=c++17", source.toString(), "-o", executable.toString())
                    .redirectErrorStream(true)
                    .start();
            String compileOutput = new String(compile.getInputStream().readAllBytes());
            assertThat(compile.waitFor()).withFailMessage(compileOutput).isZero();

            assertThat(run(List.of(executable.toString()), "a b c", null, null))
                    .isEqualTo("a b c");
        } finally {
            delete(directory);
        }
    }

    private String run(
            List<String> command,
            String stdin,
            String sourceCode,
            String sourceName
    ) throws Exception {
        Path directory = Files.createTempDirectory("generated-code-test-");
        try {
            if (sourceCode != null) {
                Files.writeString(directory.resolve(sourceName), sourceCode);
            }
            Process process = new ProcessBuilder(command)
                    .directory(directory.toFile())
                    .redirectErrorStream(true)
                    .start();
            process.getOutputStream().write(stdin.getBytes());
            process.getOutputStream().close();
            String output = new String(process.getInputStream().readAllBytes());
            assertThat(process.waitFor()).withFailMessage(output).isZero();
            return output.trim();
        } finally {
            delete(directory);
        }
    }

    private void delete(Path directory) throws Exception {
        Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}
