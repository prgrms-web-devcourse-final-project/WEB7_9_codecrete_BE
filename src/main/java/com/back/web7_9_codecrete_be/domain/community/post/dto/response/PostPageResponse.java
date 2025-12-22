package com.back.web7_9_codecrete_be.domain.community.post.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class PostPageResponse<T> {

    private List<T> content;      // 실제 데이터
    private int page;             // 현재 페이지 (1-based)
    private int size;             // 페이지 크기
    private int totalPages;       // 전체 페이지 수
    private long totalElements;   // 전체 데이터 수
    private boolean hasNext;      // 다음 페이지 존재 여부

    public static <T> PostPageResponse<T> from(Page<T> pageResult) {
        return PostPageResponse.<T>builder()
                .content(pageResult.getContent())
                .page(pageResult.getNumber() + 1) // 0 → 1
                .size(pageResult.getSize())
                .totalPages(pageResult.getTotalPages())
                .totalElements(pageResult.getTotalElements())
                .hasNext(pageResult.hasNext())
                .build();
    }
}
