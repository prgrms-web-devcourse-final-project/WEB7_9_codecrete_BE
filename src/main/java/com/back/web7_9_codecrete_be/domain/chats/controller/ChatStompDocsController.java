package com.back.web7_9_codecrete_be.domain.chats.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.web7_9_codecrete_be.domain.chats.dto.request.ChatMessageRequest;
import com.back.web7_9_codecrete_be.domain.chats.dto.response.ChatMessageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/docs/chat")
@Tag(name = "Chat STOMP", description = "WebSocket / STOMP 채팅 프로토콜 문서. 문서용 API. 사용X")
public class ChatStompDocsController {

	@Operation(
		summary = "채팅 메시지 전송 (STOMP)",
		description = """
        ### 📡 WebSocket STOMP 채팅 메시지 전송

        #### 1️⃣ WebSocket Endpoint
        ```
        ws://localhost:8080/ws-chat
        or
        wss://api.naeconcertbutakhae.shop/ws-chat
        ```

        #### 2️⃣ SEND Destination
        ```
        /app/chat/send
        ```

        #### 3️⃣ SUBSCRIBE Destination
        ```
        /topic/chat/{concertId}
        ```

        #### 4️⃣ SEND Payload
        ```json
        {
          "concertId": 1,
          "content": "안녕하세요!"
        }
        ```

        #### 5️⃣ SUBSCRIBE Response
        ```json
        {
          "concertId": 1,
          "senderId": 10,
          "senderName": "테스트 유저",
          "content": "안녕하세요!",
          "sentAt": "2025-12-23T15:30:00"
        }
        ```
        """
	)
	@GetMapping("/stomp")
	public void stompChatGuide() {}

	@Operation(
		summary = "STOMP 채팅 메시지 전송 규격",
		description = """
        WebSocket + STOMP 기반 채팅 메시지 전송 규격입니다.

        - 실제 사용되는 HTTP API 아닙니다.
        - Swagger 문서용
        """,
		requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "STOMP 메세지 SEND하면 전달되는 요청 데이터",
			required = true,
			content = @Content(
				schema = @Schema(implementation = ChatMessageRequest.class)
			)
		),
		responses = {
			@ApiResponse(
				responseCode = "200",
				description = "STOMP SUBSCRIBE로 수신되는 메시지",
				content = @Content(
					schema = @Schema(implementation = ChatMessageResponse.class)
				)
			)
		}
	)
	@GetMapping("/message-schema")
	public ChatMessageResponse messageSchema() {
		return null; // 실제 반환 목적 X
	}
}

