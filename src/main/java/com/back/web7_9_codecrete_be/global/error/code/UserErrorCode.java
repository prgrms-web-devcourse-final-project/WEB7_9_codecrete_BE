package com.back.web7_9_codecrete_be.global.error.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {

    // 1xx - User 상태 / 중복
    NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "U-100", "이미 사용 중인 닉네임입니다."),
    USER_DELETED(HttpStatus.FORBIDDEN, "U-101", "탈퇴한 사용자입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U-102", "사용자를 찾을 수 없습니다."),
    USER_RESTORE_EXPIRED(HttpStatus.BAD_REQUEST, "U-103", "계정 복구 가능 기간이 만료되었습니다."),
    USER_NOT_DELETED(HttpStatus.BAD_REQUEST, "U-104", "탈퇴 상태의 계정만 복구할 수 있습니다."),
    INVALID_RESTORE_TOKEN(HttpStatus.BAD_REQUEST, "U-105", "유효하지 않거나 만료된 복구 링크입니다."),

    // 2xx - 인증 / 비밀번호
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "U-120", "현재 비밀번호가 일치하지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

