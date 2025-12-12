package com.back.web7_9_codecrete_be.global.error.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MailErrorCode implements ErrorCode{

    // 메일 관련
    MAIL_SEND_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, "M-100", "메일 전송에 실패했습니다."),
    INVALID_EMAIL_ADDRESS(HttpStatus.BAD_REQUEST, "M-101", "유효하지 않은 이메일 주소입니다."),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "M-102", "인증 코드가 만료되었습니다."),
    VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "M-103", "인증 코드가 일치하지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
