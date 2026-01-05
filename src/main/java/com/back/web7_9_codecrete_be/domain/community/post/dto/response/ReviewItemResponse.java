package com.back.web7_9_codecrete_be.domain.community.post.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReviewItemResponse {

    private Long postId;
    private Integer rating;
    private Long likeCount;
    private List<String> tags;
}
