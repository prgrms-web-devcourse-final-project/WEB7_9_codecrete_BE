package com.back.web7_9_codecrete_be.domain.community.post.entity;

import com.back.web7_9_codecrete_be.global.error.code.PostErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import com.fasterxml.jackson.annotation.JsonCreator;

public enum PostCategory {
    NOTICE,
    REVIEW,
    TRADE,
    PHOTO;

    @JsonCreator
    public static PostCategory from(String value) {
        try {
            return PostCategory.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(PostErrorCode.INVALID_POST_CATEGORY);
        }
    }
}
