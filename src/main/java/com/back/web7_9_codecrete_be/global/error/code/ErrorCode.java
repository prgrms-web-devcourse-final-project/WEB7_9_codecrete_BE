package com.back.web7_9_codecrete_be.global.error.code;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

	String getMessage();
	HttpStatus getStatus();
	String getCode();

}
