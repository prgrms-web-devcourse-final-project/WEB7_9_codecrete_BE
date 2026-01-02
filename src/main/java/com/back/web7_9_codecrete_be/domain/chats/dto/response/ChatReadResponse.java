package com.back.web7_9_codecrete_be.domain.chats.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "채팅 메시지 조회 응답")
public class ChatReadResponse {

	@Schema(description = "Redis Stream 메시지 ID (before로 전달하면, 해당 메시지 이전 메시지를 조회할 수 있습니다)", example = "1700000123456-0")
	private String messageId;

	@Schema(description = "공연 ID", example = "1")
	private Long concertId;

	@Schema(description = "발신자 ID", example = "2")
	private Long senderId;

	@Schema(description = "발신자 닉네임", example = "테스트 유저")
	private String senderName;

	@Schema(description = "메시지 내용", example = "안녕하세요")
	private String content;

	@Schema(description = "전송 시각", example = "2026-01-02T12:13:13.1588173")
	private LocalDateTime sentDate;

	@Schema(description = "프로필 이미지", example = "https://example.com/profile.jpg")
	private String profileImage;
}
