package com.back.web7_9_codecrete_be.global.redis;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.back.web7_9_codecrete_be.domain.chats.dto.response.ChatMessageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatMessageSubscriber implements MessageListener {

	private final SimpMessagingTemplate messagingTemplate;
	private final ObjectMapper objectMapper;

	@Override
	public void onMessage(
		Message message,
		byte[] pattern
	) {
		try {
			ChatMessageResponse response =
				objectMapper.readValue(
					message.getBody(),
					ChatMessageResponse.class
				);

			messagingTemplate.convertAndSend(
				"/topic/chat/" + response.getConcertId(),
				response
			);

		} catch (Exception e) {
			log.error("[Redis Pub/Sub] 채팅 메시지 역직렬화 실패", e);
		}
	}
}

