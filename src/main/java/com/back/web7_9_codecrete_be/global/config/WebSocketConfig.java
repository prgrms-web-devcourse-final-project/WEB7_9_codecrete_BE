package com.back.web7_9_codecrete_be.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.back.web7_9_codecrete_be.global.websocket.ChatStompHandler;
import com.back.web7_9_codecrete_be.global.websocket.CustomHandshakeInterceptor;
import com.back.web7_9_codecrete_be.global.websocket.JwtHandshakeHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final JwtHandshakeHandler jwtHandshakeHandler;
	private final ChatStompHandler chatStompHandler;
	private final CustomHandshakeInterceptor customHandshakeInterceptor;

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {

		config.enableSimpleBroker("/topic");
		config.setApplicationDestinationPrefixes("/app");

	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {

		log.info("WebSocket 엔드포인트 등록: /ws-chat");

		registry.addEndpoint("/ws-chat")
			.setHandshakeHandler(jwtHandshakeHandler)
			.addInterceptors(customHandshakeInterceptor)
			.setAllowedOriginPatterns("http://localhost:3000",
				"https://www.naeconcertbutakhae.shop");

	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(chatStompHandler);
	}
}
