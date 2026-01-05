package com.back.web7_9_codecrete_be.domain.community.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "공연 후기 목록 및 요약 정보 응답 DTO")
public class ConcertReviewListResponse {

    @Schema(description = "후기 요약 정보 (전체 개수, 평균 평점, 평점 분포)")
    private ReviewSummary summary;

    @Schema(description = "공연 후기 목록")
    private List<ReviewItemResponse> reviews;
}
