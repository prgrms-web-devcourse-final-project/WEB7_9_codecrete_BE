package com.back.web7_9_codecrete_be.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

	EMAIL_DUPLICATED(HttpStatus.CONFLICT, "U-100", "이미 사용중인 이메일입니다."),
	NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "U-101", "이미 사용중인 닉네임입니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;

}
