package com.back.web7_9_codecrete_be.domain.community.post.dto.response;

import com.back.web7_9_codecrete_be.domain.community.post.entity.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "마이페이지 게시글 카드 응답 DTO")
public class MyPagePostResponse {

    @Schema(description = "게시글 ID")
    private Long postId;

    @Schema(description = "작성자 사용자 ID")
    private Long userId;

    @Schema(description = "게시글 카테고리")
    private PostCategory category;

    @Schema(description = "게시글 제목")
    private String title;

    @Schema(description = "게시글 내용")
    private String content;

    @Schema(description = "연관 콘서트 ID")
    private Long concertId;

    @Schema(description = "작성일")
    private LocalDateTime createdAt;

    // 후기글 전용
    @Schema(description = "별점", nullable = true)
    private Double rating;
}
