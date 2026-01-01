package com.back.web7_9_codecrete_be.global.websocket;

import org.springframework.stereotype.Component;

@Component
public class ServerInstanceId {

	public static final String ID = java.util.UUID.randomUUID().toString();
}
