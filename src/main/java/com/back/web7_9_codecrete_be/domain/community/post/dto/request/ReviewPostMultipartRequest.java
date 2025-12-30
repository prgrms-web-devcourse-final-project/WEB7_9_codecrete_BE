package com.back.web7_9_codecrete_be.domain.community.post.dto.request;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@Schema(description = "후기 게시글 작성 요청 (multipart/form-data)")
public class ReviewPostMultipartRequest {

    @NotNull(message = "콘서트 ID는 필수입니다.")
    @Schema(
            description = "후기를 작성할 콘서트 ID",
            example = "1"
    )
    private Long concertId;

    @NotBlank(message = "제목은 필수입니다.")
    @Schema(
            description = "후기 게시글 제목",
            example = "아이유 콘서트 후기"
    )
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    @Schema(
            description = "후기 게시글 내용",
            example = "라이브가 정말 미쳤습니다. 음향도 최고였어요."
    )
    private String content;

    @NotNull(message = "평점은 필수입니다.")
    @Min(value = 0, message = "평점은 0 이상이어야 합니다.")
    @Max(value = 5, message = "평점은 5 이하여야 합니다.")
    @Schema(
            description = "콘서트 평점 (0~5)",
            example = "5"
    )
    private Integer rating;

    @Parameter(
            description = "후기 이미지 파일 (다중 업로드 가능)",
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    array = @ArraySchema(
                            schema = @Schema(type = "string", format = "binary")
                    )
            )
    )
    private List<MultipartFile> images;
}
