package com.back.web7_9_codecrete_be.domain.community.post.dto.request;

import com.back.web7_9_codecrete_be.domain.community.post.entity.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "게시글 수정 요청 DTO")
public class PostUpdateRequest {

    @NotNull(message = "카테고리는 필수입니다.")
    @Schema(
            description = "게시글 카테고리",
            example = "NOTICE",
            allowableValues = {"NOTICE", "REVIEW","JOIN", "TRADE", "PHOTO"}
    )
    private PostCategory category;

    @NotBlank(message = "제목은 필수입니다.")
    @Schema(description = "게시글 제목", example = "예매 꿀팁 공유합니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    @Schema(description = "게시글 내용", example = "이렇게 하면 예매 성공 확률 올라갑니다.")
    private String content;
}