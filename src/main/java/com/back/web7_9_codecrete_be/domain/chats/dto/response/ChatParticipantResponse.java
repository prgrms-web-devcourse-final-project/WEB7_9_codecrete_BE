package com.back.web7_9_codecrete_be.domain.chats.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatParticipantResponse {

	@Schema(description = "유저 ID", example = "1")
	private Long userId;

	@Schema(description = "유저 닉네임", example = "테스트 유저")
	private String nickname;

	@Schema(description = "프로필 이미지 URL", example = "https://example.com/profile/1.png")
	private String profileImage;
}
