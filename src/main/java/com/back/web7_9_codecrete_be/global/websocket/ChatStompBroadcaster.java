package com.back.web7_9_codecrete_be.global.websocket;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatStompBroadcaster {

	private final ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider;

	public void broadcast(String destination, Object payload) {

		SimpMessagingTemplate messagingTemplate =
			messagingTemplateProvider.getIfAvailable();

		if (messagingTemplate == null) {
			return; // WebSocket 브로커 아직 준비 안 됨
		}

		messagingTemplate.convertAndSend(destination, payload);
	}
}


