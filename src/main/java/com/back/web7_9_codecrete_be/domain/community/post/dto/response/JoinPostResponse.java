package com.back.web7_9_codecrete_be.domain.community.post.dto.response;

import com.back.web7_9_codecrete_be.domain.community.post.entity.GenderPreference;
import com.back.web7_9_codecrete_be.domain.community.post.entity.JoinPost;
import com.back.web7_9_codecrete_be.domain.community.post.entity.JoinStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "구인글 응답 DTO")
public class JoinPostResponse {

    @Schema(description = "공통 게시글 정보")
    private PostResponse post;

    @Schema(description = "콘서트 ID", example = "1")
    private Long concertId;

    @Schema(description = "모집 인원", example = "4")
    private Integer maxParticipants;

    @Schema(description = "현재 참여 인원", example = "2")
    private Integer currentParticipants;

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

    @Schema(
            description = "만날 시간",
            example = "2025-01-05T18:30:00"
    )
    private LocalDateTime meetingAt;

    @Schema(
            description = "만날 장소",
            example = "잠실역 3번 출구"
    )
    private String meetingPlace;

    @Schema(
            description = "활동 태그",
            example = "[\"Dinner before\", \"Photo taking\"]"
    )
    private List<String> activityTags;

    @Schema(
            description = "모집 상태",
            allowableValues = {"OPEN", "CLOSED"},
            example = "OPEN"
    )
    private JoinStatus status;

    public static JoinPostResponse from(JoinPost joinPost) {
        return JoinPostResponse.builder()
                .post(PostResponse.from(joinPost.getPost()))
                .concertId(joinPost.getConcertId())
                .maxParticipants(joinPost.getMaxParticipants())
                .currentParticipants(joinPost.getCurrentParticipants())
                .genderPreference(joinPost.getGenderPreference())
                .ageRangeMin(joinPost.getAgeRangeMin())
                .ageRangeMax(joinPost.getAgeRangeMax())
                .meetingAt(joinPost.getMeetingAt())
                .meetingPlace(joinPost.getMeetingPlace())
                .activityTags(joinPost.getActivityTags())
                .status(joinPost.getStatus())
                .build();
    }
}
