package com.back.web7_9_codecrete_be.domain.community.post.dto.response;

import com.back.web7_9_codecrete_be.domain.community.post.entity.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "마이페이지 - 내가 좋아요한 게시글 응답 DTO")
public class MyLikedPostResponse {

    @Schema(description = "게시글 ID", example = "12")
    private Long postId;

    @Schema(description = "작성자 사용자 ID", example = "7")
    private Long userId;

    @Schema(description = "게시글 카테고리", example = "REVIEW")
    private PostCategory category;

    @Schema(description = "게시글 제목", example = "재즈 공연 후기")
    private String title;

    @Schema(description = "게시글 내용", example = "공연 분위기가 정말 좋았습니다.")
    private String content;

    @Schema(description = "연관 콘서트 ID", example = "5")
    private Long concertId;

    @Schema(description = "좋아요 누른 시점", example = "2025-01-11T09:20:00")
    private LocalDateTime likedAt;
}
