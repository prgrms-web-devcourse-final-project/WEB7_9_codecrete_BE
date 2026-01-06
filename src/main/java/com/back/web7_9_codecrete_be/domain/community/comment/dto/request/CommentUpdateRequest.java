package com.back.web7_9_codecrete_be.domain.community.comment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "댓글 수정 요청 DTO")
public class CommentUpdateRequest {

    @NotBlank(message = "댓글 내용은 비어 있을 수 없습니다.")
    @Schema(description = "수정할 댓글 내용", example = "내용을 수정했습니다.")
    private String content;
}
