package com.back.web7_9_codecrete_be.domain.plans.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "플랜 공유 링크 응답 DTO")
public class PlanShareLinkResponse {
    
    @Schema(description = "플랜 ID", example = "1")
    private Long planId;

    @Schema(description = "공유 토큰 (UUID 기반 13자)", example = "550e8400-e29b")
    private String shareToken;

    @Schema(description = "공유 링크", example = "/plans/share/550e8400-e29b")
    private String shareLink;
}

