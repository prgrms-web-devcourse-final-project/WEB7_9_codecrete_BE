package com.back.web7_9_codecrete_be.domain.chats.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
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

		chatMessageService.handleSend(message, principal);
	}

	@MessageMapping("/chat/status")
	public void getInitialCount(@Payload Map<String, Object> payload) {

		Object concertIdObj = payload.get("concertId");

		if (concertIdObj != null) {
			Long concertId = Long.valueOf(concertIdObj.toString());
			log.info("[CHAT STATUS] 인원수 초기 요청 수신: concertId={}", concertId);

			chatMessageService.broadcastUserCount(concertId);
		}
	}
}

