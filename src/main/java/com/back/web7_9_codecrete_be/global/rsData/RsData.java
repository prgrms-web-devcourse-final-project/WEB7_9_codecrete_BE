package com.back.web7_9_codecrete_be.global.rsData;

import org.springframework.http.HttpStatus;

import com.back.web7_9_codecrete_be.global.error.code.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class RsData<T> {

	private int status;
	private String resultCode;
	private String msg;
	private T data;

	public RsData(int status, String resultCode, String msg) {
		this.status = status;
		this.resultCode = resultCode;
		this.msg = msg;
		this.data = null;
	}

	public static <T> RsData<T> success(HttpStatus status, String message, T data) {
		return new RsData<>(status.value(), status.name(), message, data);
	}

	public static <T> RsData<T> success(HttpStatus status, T data) {
		return new RsData<>(status.value(), status.name(), "정상적으로 처리되었습니다.", data);
	}

	public static <T> RsData<T> success(String message, T data) {
		return new RsData<>(200, "OK", message, data);
	}

	public static <T> RsData<T> success(T data) {
		return new RsData<>(200, "OK", "정상적으로 처리되었습니다.", data);
	}

	public static <T> RsData<T> error(ErrorCode errorCode) {
		return new RsData<>(errorCode.getStatus().value(), errorCode.getCode(), errorCode.getMessage());
	}

}
