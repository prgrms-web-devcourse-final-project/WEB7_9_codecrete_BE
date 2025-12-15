package com.back.web7_9_codecrete_be.global.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    //임시 구현
    String upload(MultipartFile file);
}
