package com.back.web7_9_codecrete_be.domain.plans.dto.response;

import com.back.web7_9_codecrete_be.domain.plans.entity.PlanParticipant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "참가자 목록 조회 응답 DTO")
public class PlanParticipantListResponse {
    @Schema(description = "계획 ID", example = "1")
    private Long planId;
    @Schema(description = "참가자 목록")
    private List<ParticipantDetailInfo> participants;

    @Getter
    @Builder
    @Schema(description = "참가자 상세 정보")
    public static class ParticipantDetailInfo {
        @Schema(description = "참가자 정보 ID", example = "1")
        private Long participantId;
        @Schema(description = "사용자 ID", example = "1")
        private Long userId;
        @Schema(description = "사용자 닉네임", example = "홍길동")
        private String nickname;
        @Schema(description = "사용자 이메일", example = "user@example.com")
        private String email;
        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
        private String profileImage;
        @Schema(description = "초대 상태", example = "ACCEPTED")
        private PlanParticipant.InviteStatus inviteStatus;
        @Schema(description = "참여자 역할", example = "EDITOR")
        private PlanParticipant.ParticipantRole role;
    }
}

