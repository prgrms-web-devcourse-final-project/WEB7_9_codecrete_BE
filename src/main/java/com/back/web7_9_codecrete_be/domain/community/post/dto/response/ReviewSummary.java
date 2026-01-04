package com.back.web7_9_codecrete_be.domain.community.post.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class ReviewSummary {

    private long totalCount;
    private double averageRating;
    private Map<Integer, Long> ratingDistribution;
}
