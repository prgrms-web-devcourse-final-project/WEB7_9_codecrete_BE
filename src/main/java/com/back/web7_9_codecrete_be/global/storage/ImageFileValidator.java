package com.back.web7_9_codecrete_be.global.storage;

import com.back.web7_9_codecrete_be.global.error.code.FileErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Component
public class ImageFileValidator {

    private final Tika tika = new Tika();
    private static final long MAX_PROFILE_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB

    public void validateImageFile(MultipartFile file) {

        // 파일 비어있는지 검사
        if (file == null || file.isEmpty()) {
            throw new BusinessException(FileErrorCode.FILE_EMPTY);
        }

        // 파일 크기 검사
        if (file.getSize() > MAX_PROFILE_IMAGE_SIZE) {
            throw new BusinessException(FileErrorCode.PROFILE_IMAGE_SIZE_EXCEEDED);
        }

        // 실제 MIME 타입 분석
        String detectedMimeType;
        try (InputStream is = file.getInputStream()) {
            detectedMimeType = tika.detect(is);
        } catch (IOException e) {
            throw new BusinessException(FileErrorCode.FILE_ANALYZE_FAILED);
        }

        ImageMimeType imageMimeType = ImageMimeType.from(detectedMimeType);

        // 확장자 추출
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            throw new BusinessException(FileErrorCode.EXTENSION_MISMATCH);
        }

        String extension = filename.substring(filename.lastIndexOf('.') + 1);

        // 확장자 ↔ MIME 타입 매칭
        if (!imageMimeType.matches(extension)) {
            throw new BusinessException(FileErrorCode.EXTENSION_MISMATCH);
        }
    }
}
