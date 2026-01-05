package com.back.web7_9_codecrete_be.domain.community.post.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ConcertReviewListResponse {

    private ReviewSummary summary;
    private List<ReviewItemResponse> reviews;
}
