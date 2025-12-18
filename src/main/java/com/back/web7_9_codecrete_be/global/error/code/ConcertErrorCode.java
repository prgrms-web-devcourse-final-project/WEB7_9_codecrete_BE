package com.back.web7_9_codecrete_be.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ConcertErrorCode implements ErrorCode {

    // C-10* 공연 탐색 관련
    CONCERT_NOT_FOUND(HttpStatus.NOT_FOUND,"C-101","공연을 찾을 수 없습니다."),
    KEYWORD_IS_NULL(HttpStatus.BAD_REQUEST,"C-102","검색 키워드를 입력해주세요."),
    LIKE_CONFLICT(HttpStatus.CONFLICT,"C-131","이미 좋아요를 누른 공연입니다."),
    NOT_FOUND_CONCERTLIKE(HttpStatus.NOT_FOUND,"C-130","좋아요를 누르지 않은 공연입니다.")
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
