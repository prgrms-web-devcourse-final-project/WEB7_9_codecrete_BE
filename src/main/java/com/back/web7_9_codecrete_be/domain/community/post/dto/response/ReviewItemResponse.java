package com.back.web7_9_codecrete_be.domain.community.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "후기 게시글 목록 아이템 응답 DTO")
public class ReviewItemResponse {

    @Schema(description = "게시글 ID", example = "101")
    private Long postId;

    @Schema(description = "작성자 유저 ID", example = "12")
    private Long userId;

    @Schema(description = "후기 제목", example = "공연 너무 좋았어요!")
    private String title;

    @Schema(description = "후기 내용", example = "라이브 음향이 정말 좋았습니다.")
    private String content;

    @Schema(description = "평점 (0~5)", example = "4")
    private Integer rating;

    @Schema(description = "좋아요 수", example = "23")
    private Long likeCount;

    @Schema(description = "후기 태그 목록", example = "[\"락\", \"라이브\", \"음향\"]")
    private List<String> tags;

    @Schema(description = "게시글 작성일시", example = "2025-12-28T14:32:10")
    private LocalDateTime createdDate;
}
