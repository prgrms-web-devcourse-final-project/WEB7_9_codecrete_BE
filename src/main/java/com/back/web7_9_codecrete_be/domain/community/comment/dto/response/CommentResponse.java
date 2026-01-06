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

    @Schema(description = "수정일시", example = "2025-01-04T10:15:00")
    private LocalDateTime modifiedDate;

    @Schema(description = "수정 여부", example = "true")
    private Boolean isEdited;

    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .commentId(comment.getCommentId())
                .userId(comment.getUserId())
                .content(comment.getContent())
                .createdDate(comment.getCreatedDate())
                .modifiedDate(comment.getModifiedDate())
                .isEdited(isEdited(comment))
                .build();
    }

    private static boolean isEdited(Comment comment) {
        if (comment.getModifiedDate() == null) {
            return false;
        }

        LocalDateTime created = comment.getCreatedDate().withNano(0);
        LocalDateTime modified = comment.getModifiedDate().withNano(0);

        return !created.isEqual(modified);
    }
}
