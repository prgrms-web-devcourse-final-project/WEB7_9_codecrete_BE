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
@Tag(name = "Chat")
public class ChatStompDocsController {

	@Operation(
		summary = "채팅 메시지 전송 (WebSocket / STOMP 채팅 프로토콜 문서. 문서용 API. 사용X)",
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
		summary = "STOMP 채팅 메시지 전송 규격(문서용 API. 사용X)",
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


	@Operation(
		summary = "실시간 채팅 접속자 수 집계 (STOMP 전용, 문서용 API. 사용X)",
		description = """
        ### 👥 실시간 채팅 접속자 수 집계

        - 동일 유저의 여러 탭은 **1명으로 집계**
        - 모든 탭이 닫혔을 때만 접속자 수 감소
        - 접속 / 퇴장 / 초기 요청 시 자동 브로드캐스트

        ---

        ### 1️⃣ CONNECT 시 자동 집계
        - STOMP CONNECT 시 `concertId` 기준으로 접속자 등록
        - 접속 즉시 현재 인원 수가 브로드캐스트됩니다.
        
        #### CONNECT Headers
        - 접속자 수 집계를 위해 `concertId`를 CONNECT 헤더로 전달
		```text
		concertId: number
		```

        ### 2️⃣ 초기 접속자 수 요청 (SEND)
        ```
        /app/chat/status
        ```


        - 채팅방 최초 진입 시 프론트에서 1회 호출
        - 서버가 Redis 기준 현재 접속자 수 계산 후 브로드캐스트

        ### 3️⃣ SUBSCRIBE Destination
        ```
        /topic/chat/{concertId}/count
        ```

        #### SUBSCRIBE Response
        ```json
        5
        ```

        - 숫자(Number) 형태의 현재 접속자 수
        - 접속 / 퇴장 / 초기 요청 시마다 자동 수신

        ### 4️⃣ DISCONNECT 처리
        - STOMP DISCONNECT 시 자동 처리
        - 동일 유저의 모든 탭이 닫힌 경우에만 접속자 수 감소
        """
	)
	@GetMapping("/user-count")
	public void chatUserCountGuide() {}

	@Operation(
		summary = "실시간 채팅 참여자 목록 조회 (STOMP 전용, 문서용 API. 사용X)",
		description = """
        ### 👤 실시간 채팅 참여자 목록

        - 현재 채팅방에 **접속 중인 유저 목록**
        - 동일 유저의 여러 탭은 **1명으로 집계**
        - 접속 / 퇴장 / 초기 진입 시 자동 브로드캐스트

        ---

        ### 1️⃣ CONNECT
        - STOMP CONNECT 시 `concertId` 헤더 전달
        - 서버는 해당 채팅방 Presence에 유저를 등록
        
		#### CONNECT Headers
		```text
		concertId: number
		```

        ### 2️⃣ SUBSCRIBE Destination
        ```
        /topic/chat/{concertId}/users
        ```

        ### 3️⃣ SUBSCRIBE Response
        ```json
        [
          {
            "userId": 1,
            "nickname": "어드민유저",
            "profileImage": "https://cdn.example.com/profile/1.png"
          },
          {
            "userId": 2,
            "nickname": "테스트유저",
            "profileImage": "https://cdn.example.com/profile/2.png"
          }
        ]
        ```

        - Array 형태의 참여자 목록
        """
	)
	@GetMapping("/users")
	public void chatUserListGuide() {}
}

