package com.back.web7_9_codecrete_be.global.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    // 파일 업로드 메서드
    String upload(MultipartFile file, String basePath);

    void delete(String fileUrl);
}
