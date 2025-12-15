package com.back.web7_9_codecrete_be.global.error.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {

    // 1xx - User 상태 / 중복
    NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "U-101", "이미 사용 중인 닉네임입니다."),
    USER_DELETED(HttpStatus.FORBIDDEN, "U-102", "탈퇴한 사용자입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U-103", "사용자를 찾을 수 없습니다."),

    // 3xx - 입력값 / 파일
    INVALID_PROFILE_IMAGE(HttpStatus.BAD_REQUEST, "U-301", "유효하지 않은 프로필 이미지입니다."),

    // 2xx - 인증 / 비밀번호
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "U-201", "현재 비밀번호가 일치하지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

