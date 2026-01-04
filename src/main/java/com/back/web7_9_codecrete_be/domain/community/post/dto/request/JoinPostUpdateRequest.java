package com.back.web7_9_codecrete_be.domain.community.post.dto.request;

import com.back.web7_9_codecrete_be.domain.community.post.entity.GenderPreference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Schema(description = "구인글 수정 요청")
public class JoinPostUpdateRequest {

    @NotNull(message = "콘서트 ID는 필수입니다.")
    @Schema(
            description = "연관된 콘서트 ID",
            example = "1"
    )
    private Long concertId;

    @NotBlank(message = "제목은 필수입니다.")
    @Schema(
            description = "구인글 제목",
            example = "아이유 콘서트 같이 가실 분 구해요!"
    )
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    @Schema(
            description = "구인글 내용",
            example = "혼자 가기 아쉬워서 같이 즐기실 분 찾습니다."
    )
    private String content;

    @NotNull(message = "모집 인원은 필수입니다.")
    @Min(value = 1, message = "모집 인원은 최소 1명 이상이어야 합니다.")
    @Schema(description = "모집 인원", example = "4")
    private Integer maxParticipants;

    @Schema(
            description = "성별 선호",
            allowableValues = {"MALE", "FEMALE", "ANY"},
            example = "ANY"
    )
    private GenderPreference genderPreference;

    @Schema(description = "연령대 최소", example = "20")
    private Integer ageRangeMin;

    @Schema(description = "연령대 최대", example = "35")
    private Integer ageRangeMax;

    @Schema(description = "만날 시간", example = "2025-01-05T18:30:00")
    private LocalDateTime meetingAt;

    @Schema(description = "만날 장소", example = "잠실역 3번 출구")
    private String meetingPlace;

    @Schema(
            description = "활동 태그",
            example = "[\"Dinner before\", \"Photo taking\"]"
    )
    private List<String> activityTags;
}
