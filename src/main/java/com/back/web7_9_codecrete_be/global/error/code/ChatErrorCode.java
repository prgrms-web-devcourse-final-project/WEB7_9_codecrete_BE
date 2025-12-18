package com.back.web7_9_codecrete_be.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements ErrorCode {

	CHAT_NOT_AVAILABLE(HttpStatus.FORBIDDEN, "CH-100", "채팅 가능 기간이 아닙니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
