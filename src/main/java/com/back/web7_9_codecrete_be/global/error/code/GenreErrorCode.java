package com.back.web7_9_codecrete_be.global.error.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GenreErrorCode implements ErrorCode {

    GENRE_NOT_FOUND(HttpStatus.NOT_FOUND, "G-100", "장르를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
