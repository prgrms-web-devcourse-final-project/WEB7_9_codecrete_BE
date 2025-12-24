package com.back.web7_9_codecrete_be.domain.chats.service;

import java.security.Principal;
import java.time.LocalDateTime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.back.web7_9_codecrete_be.domain.chats.dto.ChatMessageRequest;
import com.back.web7_9_codecrete_be.domain.chats.dto.ChatMessageResponse;
import com.back.web7_9_codecrete_be.domain.chats.repository.ChatStreamRepository;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.domain.users.repository.UserRepository;
import com.back.web7_9_codecrete_be.global.error.code.AuthErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

	private final UserRepository userRepository;
	private final SimpMessagingTemplate messagingTemplate;
	private final ChatStreamRepository chatStreamRepository;
	private final ChatPolicyService chatPolicyService;

	public void sendMessage(ChatMessageRequest request, Principal principal) {

		String email = principal.getName();

		// TODO: 캐싱처리
		User user = userRepository.findByEmail(email)
			.orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

		ChatMessageResponse response = new ChatMessageResponse(
			request.getConcertId(),
			user.getId(),
			user.getNickname(),
			request.getContent(),
			LocalDateTime.now()
		);

		log.info("[SEND MESSAGE] From User ID: {}, Content: {}", user.getId(), request.getContent());

		chatStreamRepository.save(response);

		// WebSocket 브로드캐스트
		messagingTemplate.convertAndSend(
			"/topic/chat/" + request.getConcertId(),
			response
		);
	}
}
