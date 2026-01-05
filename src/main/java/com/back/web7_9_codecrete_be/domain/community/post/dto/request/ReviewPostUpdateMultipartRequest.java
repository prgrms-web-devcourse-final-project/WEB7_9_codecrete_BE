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
@Schema(description = "후기 게시글 수정 요청 (multipart/form-data)")
public class ReviewPostUpdateMultipartRequest {

    @NotNull(message = "콘서트 ID는 필수입니다.")
    @Schema(
            description = "연관된 콘서트 ID",
            example = "1"
    )
    private Long concertId;

    @NotBlank(message = "제목은 필수입니다.")
    @Schema(
            description = "수정할 후기 게시글 제목",
            example = "아이유 콘서트 후기 (수정)"
    )
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    @Schema(
            description = "수정할 후기 게시글 내용",
            example = "2층 좌석이었지만 시야도 괜찮고 전반적으로 만족스러웠어요."
    )
    private String content;

    @NotNull(message = "평점은 필수입니다.")
    @Min(value = 0, message = "평점은 0 이상이어야 합니다.")
    @Max(value = 5, message = "평점은 5 이하여야 합니다.")
    @Schema(
            description = "수정할 콘서트 평점 (0~5)",
            example = "4"
    )
    private Integer rating;

    @Schema(
            description = "후기 태그",
            example = "[\"Sound\", \"Stage\", \"Seat\"]"
    )
    private List<String> tags;

    @Schema(
            description = """
                    수정 후에도 유지할 기존 이미지 URL 목록  
                    - 프론트에서 기존 이미지 중 '삭제하지 않은 이미지'만 전달  
                    - 전달되지 않은 기존 이미지는 삭제 처리됨
                    """,
            example = "[\"https://s3.amazonaws.com/reviews/images/abc.jpg\"]"
    )
    private List<String> remainImageUrls;

    @Parameter(
            description = "새로 추가할 후기 이미지 파일 목록 (다중 업로드 가능)",
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    array = @ArraySchema(
                            schema = @Schema(type = "string", format = "binary")
                    )
            )
    )
    private List<MultipartFile> images;
}
