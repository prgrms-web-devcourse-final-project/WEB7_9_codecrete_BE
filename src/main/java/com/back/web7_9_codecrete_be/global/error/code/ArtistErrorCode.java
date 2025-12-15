package com.back.web7_9_codecrete_be.global.error.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;


@Getter
@RequiredArgsConstructor
public enum ArtistErrorCode implements ErrorCode {

    ARTIST_SEED_FAILED(HttpStatus.BAD_REQUEST, "AT-100", "아티스트 정보 저장에 실패했습니다."),
    SPOTIFY_API_ERROR(HttpStatus.BAD_GATEWAY, "AT-101", "Spotify API 연동 중 오류가 발생했습니다."),
    ARTIST_NOT_FOUND(HttpStatus.NOT_FOUND, "AT-102", "아티스트를 찾을 수 없습니다."),
    ARTIST_ALREADY_EXISTS(HttpStatus.CONFLICT, "AT-103", "이미 존재하는 아티스트입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
