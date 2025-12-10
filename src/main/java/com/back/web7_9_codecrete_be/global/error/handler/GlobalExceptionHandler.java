package com.back.web7_9_codecrete_be.global.error.handler;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import com.back.web7_9_codecrete_be.global.error.util.ErrorResponse;
import com.back.web7_9_codecrete_be.global.rsData.RsData;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<RsData<Void>> handleBusinessException(BusinessException ex) {
		log.error("BusinessException: {}", ex.getMessage(), ex);
		return ErrorResponse.build(ex.getErrorCode());
	}

}
