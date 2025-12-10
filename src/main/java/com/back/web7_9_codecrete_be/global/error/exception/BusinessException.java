package com.back.web7_9_codecrete_be.global.error.exception;

import com.back.web7_9_codecrete_be.global.error.code.ErrorCode;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

}
