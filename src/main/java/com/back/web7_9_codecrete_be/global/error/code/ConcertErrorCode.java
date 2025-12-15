package com.back.web7_9_codecrete_be.global.error.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ConcertErrorCode implements ErrorCode {

    LIKE_CONFLICT(HttpStatus.CONFLICT,"C131","이미 좋아요를 누른 공연입니다."),
    NOT_FOUND_CONCERTLIKE(HttpStatus.NOT_FOUND,"C130","좋아요를 누르지 않은 공연입니다.")
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
