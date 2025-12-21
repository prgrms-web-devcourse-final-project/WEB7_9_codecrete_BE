package com.back.web7_9_codecrete_be.global.storage;

import com.back.web7_9_codecrete_be.global.error.code.FileErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;

import java.util.Arrays;
import java.util.List;

public enum ImageMimeType {

    JPEG("image/jpeg", List.of("jpg", "jpeg")),
    PNG("image/png", List.of("png")),
    WEBP("image/webp", List.of("webp"));

    private final String mimeType;
    private final List<String> extensions;

    ImageMimeType(String mimeType, List<String> extensions) {
        this.mimeType = mimeType;
        this.extensions = extensions;
    }

    public static ImageMimeType from(String detectedMimeType) {
        return Arrays.stream(values())
                .filter(it -> it.mimeType.equals(detectedMimeType))
                .findFirst()
                .orElseThrow(() ->
                        new BusinessException(FileErrorCode.INVALID_IMAGE_TYPE)
                );
    }

    public boolean matches(String extension) {
        return extensions.contains(extension.toLowerCase());
    }
}

