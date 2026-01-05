package com.back.web7_9_codecrete_be.domain.community.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@Schema(description = "후기 게시글 요약 통계 응답 DTO")
public class ReviewSummary {

    @Schema(description = "전체 후기 개수", example = "128")
    private long totalCount;

    @Schema(description = "평균 평점", example = "4.2")
    private double averageRating;

    @Schema(
            description = "평점 분포 (key: 평점, value: 개수)",
            example = "{ \"5\": 60, \"4\": 40, \"3\": 20, \"2\": 5, \"1\": 3 }"
    )
    private Map<Integer, Long> ratingDistribution;
}
