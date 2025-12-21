package com.back.web7_9_codecrete_be.global.error.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FileErrorCode implements ErrorCode {

    FILE_EMPTY(HttpStatus.BAD_REQUEST, "F-101", "파일이 비어 있습니다."),
    INVALID_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "F-102", "허용되지 않은 이미지 파일입니다."),
    EXTENSION_MISMATCH(HttpStatus.BAD_REQUEST, "F-103", "파일 확장자와 실제 파일 타입이 일치하지 않습니다."),
    FILE_ANALYZE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F-104", "파일 분석 중 오류가 발생했습니다."),
    PROFILE_IMAGE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "F-105", "프로필 이미지 크기가 10MB를 초과했습니다."),;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
