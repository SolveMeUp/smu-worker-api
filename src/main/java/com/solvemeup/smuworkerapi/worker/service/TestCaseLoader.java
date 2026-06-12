package com.solvemeup.smuworkerapi.worker.service;

import com.solvemeup.smuworkerapi.worker.Entity.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TestCaseLoader {

    @Value("${FILE_BASE_PATH:testcase/problem}")
    private String basePath;
    private int START_NUMBER = 1;

    public List<TestCase> loadTestCases(Long problemId){
        List<TestCase> testCases= new ArrayList<>();

        try{
            Path problemPath = Paths.get(basePath, problemId.toString());
            Path inputDir = problemPath.resolve("input");
            Path outputDir = problemPath.resolve("output");

            if(!Files.exists(inputDir) || !Files.exists(outputDir)){
                log.info("테스트 케이스 디렉토리 찾을 수 없음:{} ", problemPath);
                return testCases;
            }


        int testCaseNumber = START_NUMBER;

            while (true) {
                String filename = String.format("%03d.txt", testCaseNumber);
                Path inputPath = inputDir.resolve(filename);
                Path outputPath = outputDir.resolve(filename);

                if (!Files.exists(inputPath) || !Files.exists(outputPath)) {
                    break;
                }

                String input = Files.readString(inputPath);
                String expectedOutput = Files.readString(outputPath);

                testCases.add(new TestCase(testCaseNumber, input, expectedOutput));
                testCaseNumber++;
            }
        } catch (IOException e) {
            log.error("테스트 케이스 로드 불가 {}", problemId, e);
        }
        return testCases;
    }
}
