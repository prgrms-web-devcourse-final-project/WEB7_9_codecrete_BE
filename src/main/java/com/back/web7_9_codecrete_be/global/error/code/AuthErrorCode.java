package com.back.web7_9_codecrete_be.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    // 회원가입 관련
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "A-100", "이미 사용중인 이메일입니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "A-101","이메일 인증이 완료되지 않았습니다."),

    // 로그인 관련
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "A-110", "존재하지 않는 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "A-111", "비밀번호가 일치하지 않습니다."),
    USER_INACTIVE(HttpStatus.FORBIDDEN, "A-112", "현재 비활성화된 계정입니다."),

    // 권한 관련
    UNAUTHORIZED_USER(HttpStatus.UNAUTHORIZED, "A-120", "로그인이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A-121", "해당 리소스에 접근할 권한이 없습니다."),

    // 토큰 관련 (JWT 적용 대비)
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A-130", "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A-131", "유효하지 않은 토큰입니다."),
    TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "A-132", "토큰이 존재하지 않습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;

}
