package com.back.web7_9_codecrete_be.domain.chats.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.back.web7_9_codecrete_be.domain.chats.dto.ChatMessageRequest;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {

	private final SimpMessagingTemplate messagingTemplate;

	@MessageMapping("/chat/send")
	public void send(ChatMessageRequest message) {

		messagingTemplate.convertAndSend(
			"/topic/chat/" + message.getConcertId(),
			message
		);
	}
}

