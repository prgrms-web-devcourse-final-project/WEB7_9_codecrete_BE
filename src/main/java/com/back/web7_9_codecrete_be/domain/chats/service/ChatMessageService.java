package com.back.web7_9_codecrete_be.domain.chats.service;

import java.security.Principal;
import java.time.LocalDateTime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.back.web7_9_codecrete_be.domain.chats.dto.request.ChatMessageRequest;
import com.back.web7_9_codecrete_be.domain.chats.dto.response.ChatMessageResponse;
import com.back.web7_9_codecrete_be.domain.chats.dto.response.ChatUserCache;
import com.back.web7_9_codecrete_be.domain.chats.repository.ChatStreamRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

	private final SimpMessagingTemplate messagingTemplate;
	private final ChatStreamRepository chatStreamRepository;
	private final ChatUserCacheService chatUserCacheService;

	public void sendMessage(ChatMessageRequest request, Principal principal) {

		String email = principal.getName();

		ChatUserCache chatUser = chatUserCacheService.getChatUser(email);

		ChatMessageResponse response = new ChatMessageResponse(
			request.getConcertId(),
			chatUser.getUserId(),
			chatUser.getNickname(),
			request.getContent(),
			LocalDateTime.now()
		);

		log.info("[SEND MESSAGE] From User ID: {}, Content: {}", chatUser.getUserId(), request.getContent());

		chatStreamRepository.save(response);

		// WebSocket 브로드캐스트
		messagingTemplate.convertAndSend(
			"/topic/chat/" + request.getConcertId(),
			response
		);
	}
}
