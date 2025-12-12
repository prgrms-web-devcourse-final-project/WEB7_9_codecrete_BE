package com.back.web7_9_codecrete_be.global.error.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;


@Getter
@RequiredArgsConstructor
public enum ArtistErrorCode implements ErrorCode {

    ARTIST_SEED_FAILED(HttpStatus.BAD_REQUEST, "AT-100", "아티스트 정보 저장에 실패했습니다."),
    SPOTIFY_API_ERROR(HttpStatus.BAD_GATEWAY, "AT-101", "Spotify API 연동 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
