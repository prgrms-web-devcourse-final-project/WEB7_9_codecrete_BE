package com.back.web7_9_codecrete_be.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.back.web7_9_codecrete_be.global.redis.ChatMessageSubscriber;
import com.back.web7_9_codecrete_be.global.redis.ChatPresenceSubscriber;
import com.back.web7_9_codecrete_be.global.redis.ChatPubSubChannels;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ChatRedisSubscriberConfig {

	private final RedisConnectionFactory connectionFactory;
	private final ChatMessageSubscriber chatMessageSubscriber;
	private final ChatPresenceSubscriber chatPresenceSubscriber;

	@Bean
	public RedisMessageListenerContainer chatRedisListenerContainer() {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);

		container.addMessageListener(
			chatMessageSubscriber,
			new ChannelTopic(ChatPubSubChannels.CHAT_MESSAGE)
		);

		container.addMessageListener(
			chatPresenceSubscriber,
			new ChannelTopic(ChatPubSubChannels.CHAT_PRESENCE)
		);

		return container;
	}
}
