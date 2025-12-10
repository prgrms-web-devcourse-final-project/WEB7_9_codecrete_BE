package com.back.web7_9_codecrete_be.global.rsData;

import org.springframework.http.HttpStatus;

import com.back.web7_9_codecrete_be.global.error.code.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class RsData<T> {

	private String resultCode;
	private String msg;
	private T data;

	public RsData(String resultCode, String msg) {
		this.resultCode = resultCode;
		this.msg = msg;
		this.data = null;
	}

	public static <T> RsData<T> success(HttpStatus status, String message, T data) {
		return new RsData<>(String.valueOf(status.value()), message, data);
	}

	public static <T> RsData<T> success(HttpStatus status, T data) {
		return new RsData<>(String.valueOf(status.value()), "정상적으로 처리되었습니다.", data);
	}

	public static <T> RsData<T> success(String message, T data) {
		return new RsData<>("200", message, data);
	}

	public static <T> RsData<T> success(T data) {
		return new RsData<>("200", "정상적으로 처리되었습니다.", data);
	}

	public static <T> RsData<T> error(ErrorCode errorCode) {
		return new RsData<>(errorCode.getCode(), errorCode.getMessage());
	}

	@JsonIgnore
	public int getStatusCode() {
		return Integer.parseInt(resultCode);
	}
}
