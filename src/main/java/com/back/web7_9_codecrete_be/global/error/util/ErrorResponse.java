package com.back.web7_9_codecrete_be.global.error.util;

import org.springframework.http.ResponseEntity;

import com.back.web7_9_codecrete_be.global.error.code.ErrorCode;
import com.back.web7_9_codecrete_be.global.rsData.RsData;

public class ErrorResponse {

	public static ResponseEntity<RsData<Void>> build(ErrorCode errorCode) {
		return ResponseEntity
			.status(errorCode.getStatus())
			.body(RsData.error(errorCode));
	}
}
