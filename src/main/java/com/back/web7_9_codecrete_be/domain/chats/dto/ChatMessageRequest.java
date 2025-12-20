package com.back.web7_9_codecrete_be.domain.chats.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {
	private Long concertId;
	private Long senderId;
	private String content;
	private LocalDateTime sentDate;
}
