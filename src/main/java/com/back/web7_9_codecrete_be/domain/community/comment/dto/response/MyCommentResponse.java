package com.back.web7_9_codecrete_be.domain.community.comment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "마이페이지 - 내가 작성한 댓글 응답 DTO")
public class MyCommentResponse {

    @Schema(description = "댓글 ID", example = "101")
    private Long commentId;

    @Schema(description = "원본 게시글 ID", example = "10")
    private Long postId;

    @Schema(description = "원본 게시글 제목", example = "공연 후기 공유합니다")
    private String postTitle;

    @Schema(description = "댓글 내용", example = "저도 이 공연 정말 좋았어요!")
    private String content;

    @Schema(description = "댓글 작성일시", example = "2025-01-10T14:30:00")
    private LocalDateTime createdAt;
}
