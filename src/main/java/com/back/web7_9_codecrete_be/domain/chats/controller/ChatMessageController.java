package com.back.web7_9_codecrete_be.domain.chats.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import com.back.web7_9_codecrete_be.domain.chats.dto.request.ChatMessageRequest;
import com.back.web7_9_codecrete_be.domain.chats.service.ChatMessageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageController {

	private final ChatMessageService chatMessageService;

	@MessageMapping("/chat/send")
	public void send(ChatMessageRequest message, Principal principal) {

		if (principal == null) {
			throw new IllegalStateException("Unauthenticated WebSocket access");
		}

		chatMessageService.sendMessage(message, principal);
	}
}

