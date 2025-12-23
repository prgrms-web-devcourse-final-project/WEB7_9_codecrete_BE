package com.back.web7_9_codecrete_be.global.error.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum LocationErrorCode implements ErrorCode{

    LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "L-100", "위치(좌표)를 찾을 수 없습니다."),
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "L-101" , "위치(주소)를 찾을 수 없습니다."),
    LOCATION_ALREADY_EXISTS(HttpStatus.NOT_FOUND, "L-102", "이미 저장되어있는 위치입니다."),
    INVALID_KOREA_COORDINATE(HttpStatus.NOT_FOUND, "L-103" , "한국 좌표가 아닙니다"),
    LOCATION_NOT_EXIST_IN_KAKAO(HttpStatus.NOT_FOUND, "L-104", "해당 좌표는 카카오에 등록되어있지 않습니다."),
    LOCATION_NOT_HAVE(HttpStatus.NOT_FOUND, "L-105", "저장되어있는 좌표가 없어서 삭제가 불가능합니다."),
    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "L-106", "추천 경로가 존재하지 않습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;

}
