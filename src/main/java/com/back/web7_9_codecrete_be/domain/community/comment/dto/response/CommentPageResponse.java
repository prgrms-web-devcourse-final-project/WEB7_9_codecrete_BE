package com.back.web7_9_codecrete_be.domain.community.comment.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class CommentPageResponse<T> {

    private List<T> content;      // 댓글 목록
    private int page;             // 현재 페이지 (1-based)
    private int size;             // 페이지 크기
    private int totalPages;       // 전체 페이지 수
    private long totalElements;   // 전체 댓글 수
    private boolean hasNext;      // 다음 페이지 존재 여부

    public static <T> CommentPageResponse<T> from(Page<T> pageResult) {
        return CommentPageResponse.<T>builder()
                .content(pageResult.getContent())
                .page(pageResult.getNumber() + 1) // 0-based → 1-based
                .size(pageResult.getSize())
                .totalPages(pageResult.getTotalPages())
                .totalElements(pageResult.getTotalElements())
                .hasNext(pageResult.hasNext())
                .build();
    }
}
