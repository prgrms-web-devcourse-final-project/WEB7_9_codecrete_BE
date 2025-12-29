package com.back.web7_9_codecrete_be.domain.chats.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "채팅 메시지 전송 요청")
public class ChatMessageRequest {

	@Schema(description = "공연 ID", example = "1")
	private Long concertId;
	@Schema(description = "메시지 내용", example = "안녕하세요")
	private String content;
}
