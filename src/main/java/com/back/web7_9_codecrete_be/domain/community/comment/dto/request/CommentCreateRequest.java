package com.back.web7_9_codecrete_be.domain.community.comment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "댓글 작성 요청 DTO")
public class CommentCreateRequest {

    @NotBlank(message = "댓글 내용은 필수입니다.")
    @Schema(description = "댓글 내용", example = "저도 이 공연 다녀왔어요!")
    private String content;
}
