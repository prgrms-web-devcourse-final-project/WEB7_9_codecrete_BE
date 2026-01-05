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
    TYPE_IS_NULL(HttpStatus.BAD_REQUEST,"C-103","타입을 입력해주세요."),
    INCORRECT_TYPE(HttpStatus.BAD_REQUEST,"C-104","타입을 정확하게 입력해주세요."),
    // C-13* 공연 좋아요 관련
    LIKE_CONFLICT(HttpStatus.CONFLICT,"C-131","이미 좋아요를 누른 공연입니다."),
    NOT_FOUND_CONCERTLIKE(HttpStatus.NOT_FOUND,"C-130","좋아요를 누르지 않은 공연입니다."),
    // C-14* 공연 예매 시간 설정 관련
    NOT_VALID_TICKETING_TIME(HttpStatus.BAD_REQUEST, "C-140","공연 예매 시간이 옳지 않습니다. 확인해 주십시오."),
    CONCERT_TICKETING_TIME_IS_NOT_AFTER_CONCERT_END_DATE(HttpStatus.BAD_REQUEST, "C-141", "공연 예매 시작 시간은 공연 시작 시간보다 이전이어야 합니다."),
    CONCERT_TICKETING_END_TIME_IS_NOT_AFTER_CONCERT_END_DATE(HttpStatus.BAD_REQUEST,"C-142","공연 예매 종료 시간은 공연 시작 시간보다 이전이어야 합니다.")
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
