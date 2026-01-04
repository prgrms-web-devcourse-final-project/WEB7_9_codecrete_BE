package com.back.web7_9_codecrete_be.domain.community.post.dto.response;

import com.back.web7_9_codecrete_be.domain.community.post.entity.Post;
import com.back.web7_9_codecrete_be.domain.community.post.entity.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "게시글 응답 DTO")
public class PostResponse {

    @Schema(description = "게시글 ID", example = "1")
    private Long postId;

    @Schema(description = "작성자 사용자 ID", example = "10")
    private Long userId;

    @Schema(description = "연관된 콘서트 ID", example = "5")
    private Long concertId;

    @Schema(description = "게시글 제목", example = "공연 정보 공유합니다")
    private String title;

    @Schema(description = "게시글 내용", example = "이번 주말 공연 정보입니다.")
    private String content;

    @Schema(description = "게시글 카테고리", example = "INFO")
    private PostCategory category;

    @Schema(description = "작성일시", example = "2025-01-01T12:00:00")
    private LocalDateTime createdDate;

    @Schema(description = "수정일시", example = "2025-01-02T09:30:00")
    private LocalDateTime modifiedDate;

    public static PostResponse from(Post post) {
        return PostResponse.builder()
                .postId(post.getPostId())
                .concertId(post.getConcertId())
                .userId(post.getUserId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
                .createdDate(post.getCreatedDate())
                .modifiedDate(post.getModifiedDate())
                .build();
    }
}
