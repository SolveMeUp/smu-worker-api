package com.solvemeup.smuworkerapi.worker.service;

import com.solvemeup.smuworkerapi.worker.Entity.ProblemMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class MetadataLoader {

    @Value("${FILE_BASE_PATH}")
    private String basePath;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProblemMetadata loadMetadata(Long problemId){
        try{
            Path metadataPath = Paths.get(basePath, problemId.toString(),"metadata.json");

            if(!Files.exists(metadataPath)){
                log.error("메타데이터 파일 없음", metadataPath);
                throw new RuntimeException("problemId로 메타데이터 파일 찾을 수 없음 " + problemId);
            }

            String json = Files.readString(metadataPath);
            ProblemMetadata metadata = objectMapper.readValue(json, ProblemMetadata.class);

            return metadata;
        } catch (IOException e){
            throw new RuntimeException("메타데이터 로드 실패", e);
        }
    }
}
