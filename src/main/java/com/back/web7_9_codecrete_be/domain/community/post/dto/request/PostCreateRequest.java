package com.back.web7_9_codecrete_be.domain.community.post.dto.request;

import com.back.web7_9_codecrete_be.domain.community.post.entity.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "게시글 작성 요청 DTO")
public class PostCreateRequest {

    @NotNull(message = "카테고리는 필수입니다.")
    @Schema(
            description = "게시글 카테고리",
            example = "REVIEW",
            allowableValues = {"NOTICE", "REVIEW", "JOIN", "TRADE", "PHOTO"}
    )
    private PostCategory category;

    @Schema(description = "연관된 콘서트 ID (선택)")
    private Long concertId;

    @NotBlank(message = "제목은 필수입니다.")
    @Schema(description = "게시글 제목", example = "콘서트 후기 남깁니다!")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    @Schema(description = "게시글 내용", example = "어제 공연 다녀왔는데 진짜 좋았어요.")
    private String content;
}
