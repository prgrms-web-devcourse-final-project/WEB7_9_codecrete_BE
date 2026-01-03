package com.back.web7_9_codecrete_be.domain.chats.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "채팅 메시지 응답")
public class ChatMessageResponse {

	@Schema(description = "공연 ID", example = "1")
	private Long concertId;

	@Schema(description = "발신자 ID", example = "2")
	private Long senderId;

	@Schema(description = "발신자 닉네임", example = "테스트 유저")
	private String senderName;

	@Schema(description = "메시지 내용", example = "안녕하세요")
	private String content;

	@Schema(description = "전송 시각", example = "2025-12-23T16:28:07.8806432")
	private LocalDateTime sentDate;

	@Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
	private String profileImage;
}
