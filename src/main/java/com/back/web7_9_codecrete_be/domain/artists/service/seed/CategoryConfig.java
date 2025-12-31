package com.back.web7_9_codecrete_be.domain.artists.service.seed;

import java.util.List;

// 카테고리별 키워드 및 상한 설정

public class CategoryConfig {
    public final List<String> keywords;
    public final int targetCount;
    public final boolean requireKorean; // 한국 음악 필터링 필요 여부

    public CategoryConfig(List<String> keywords, int targetCount, boolean requireKorean) {
        this.keywords = keywords;
        this.targetCount = targetCount;
        this.requireKorean = requireKorean;
    }
}

