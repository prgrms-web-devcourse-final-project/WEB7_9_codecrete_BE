package com.back.web7_9_codecrete_be.global.error.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PostErrorCode implements ErrorCode {

    // 게시글 조회
    POST_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "P-100",
            "게시글을 찾을 수 없습니다."
    ),

    // 게시글 작성 / 수정
    INVALID_POST_CATEGORY(
            HttpStatus.BAD_REQUEST,
            "P-110",
            "유효하지 않은 게시글 카테고리입니다."
    ),

    // 권한 관련
    NO_POST_PERMISSION(
            HttpStatus.FORBIDDEN,
            "P-120",
            "게시글에 대한 권한이 없습니다."
    ),

    // 삭제 관련
    POST_ALREADY_DELETED(
            HttpStatus.BAD_REQUEST,
            "P-130",
            "이미 삭제된 게시글입니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
