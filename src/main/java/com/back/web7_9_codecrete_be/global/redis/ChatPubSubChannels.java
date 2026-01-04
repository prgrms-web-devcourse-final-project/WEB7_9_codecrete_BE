package com.back.web7_9_codecrete_be.global.redis;

public final class ChatPubSubChannels {

	private ChatPubSubChannels() {}

	public static final String CHAT_MESSAGE = "chat:pubsub:message";
	public static final String CHAT_PRESENCE = "chat:pubsub:presence";
}
