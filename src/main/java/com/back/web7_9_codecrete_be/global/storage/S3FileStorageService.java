package com.back.web7_9_codecrete_be.global.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class S3FileStorageService implements FileStorageService {
    @Override
    public String upload(MultipartFile file) {

        // 임시 URL 생성
        String fakeFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        return "https://dummy-cdn.codecrete.com/profile/" + fakeFileName;
    }
}
