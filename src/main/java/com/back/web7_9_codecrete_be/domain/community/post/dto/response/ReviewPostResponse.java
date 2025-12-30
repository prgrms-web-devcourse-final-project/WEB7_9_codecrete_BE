package com.back.web7_9_codecrete_be.domain.community.post.dto.response;

import com.back.web7_9_codecrete_be.domain.community.post.entity.ReviewPost;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "후기 게시글 응답 DTO")
public class ReviewPostResponse {

    @Schema(description = "공통 게시글 정보")
    private PostResponse post;

    @Schema(description = "콘서트 ID", example = "100")
    private Long concertId;

    @Schema(description = "평점 (0~5)", example = "4")
    private Integer rating;

    @Schema(description = "후기 이미지 URL 목록")
    private List<String> imageUrls;

    public static ReviewPostResponse from(
            ReviewPost reviewPost,
            List<String> imageUrls
    ) {
        return ReviewPostResponse.builder()
                .post(PostResponse.from(reviewPost.getPost()))
                .concertId(reviewPost.getConcertId())
                .rating(reviewPost.getRating())
                .imageUrls(imageUrls)
                .build();
    }
}
