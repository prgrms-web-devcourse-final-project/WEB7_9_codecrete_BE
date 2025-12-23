package com.back.web7_9_codecrete_be.global.error.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCode {

    // 댓글 조회
    COMMENT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "C-100",
            "댓글을 찾을 수 없습니다."
    ),

    // 댓글 작성
    COMMENT_CONTENT_EMPTY(
            HttpStatus.BAD_REQUEST,
            "C-110",
            "댓글 내용은 비어 있을 수 없습니다."
    ),

    // 권한 관련
    NO_COMMENT_PERMISSION(
            HttpStatus.FORBIDDEN,
            "C-120",
            "댓글에 대한 권한이 없습니다."
    ),

    // 삭제 관련
    COMMENT_ALREADY_DELETED(
            HttpStatus.BAD_REQUEST,
            "C-130",
            "이미 삭제된 댓글입니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
