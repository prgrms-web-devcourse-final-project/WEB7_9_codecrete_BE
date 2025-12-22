package com.back.web7_9_codecrete_be.domain.community.comment.dto.response;

import com.back.web7_9_codecrete_be.domain.community.comment.entity.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "댓글 응답 DTO")
public class CommentResponse {

    @Schema(description = "댓글 ID", example = "5")
    private Long commentId;

    @Schema(description = "작성자 사용자 ID", example = "12")
    private Long userId;

    @Schema(description = "댓글 내용", example = "정말 공감합니다!")
    private String content;

    @Schema(description = "작성일시", example = "2025-01-03T18:40:00")
    private LocalDateTime createdDate;

    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .commentId(comment.getCommentId())
                .userId(comment.getUserId())
                .content(comment.getContent())
                .createdDate(comment.getCreatedDate())
                .build();
    }
}
