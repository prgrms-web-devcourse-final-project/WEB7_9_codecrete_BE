package com.back.web7_9_codecrete_be.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlanErrorCode implements ErrorCode {

	PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "P-100", "계획을 찾을 수 없습니다."),
	PLAN_FORBIDDEN(HttpStatus.FORBIDDEN, "P-101", "해당 계획에 접근할 수 없습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;

}

