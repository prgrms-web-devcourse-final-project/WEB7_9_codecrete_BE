package com.back.web7_9_codecrete_be.global.error.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum LocationErrorCode implements ErrorCode{

    LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "L-100", "위치를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

}
