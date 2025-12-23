package com.back.web7_9_codecrete_be.domain.chats.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.web7_9_codecrete_be.domain.chats.service.ChatRoomService;
import com.back.web7_9_codecrete_be.global.rq.Rq;
import com.back.web7_9_codecrete_be.global.rsData.RsData;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Chat Room", description = "공연 채팅방 입장 API")
@RestController
@RequestMapping("/api/v1/chat-room")
@RequiredArgsConstructor
public class ChatRoomController {

	private final ChatRoomService chatRoomService;
	private final Rq rq;

	@Operation(
		summary = "채팅방 입장",
		description = """
			공연 채팅방에 입장합니다.
			- 정책 기간(예매일 3일 전 ~ 예매일 3일 후) 안에서만 입장이 가능합니다.
			- 채팅방이 존재하지 않으면 최초 1회 Lazy 생성됩니다.

			입장 성공 후 프론트엔드는 WebSocket(STOMP)에 연결하여
			Redis Stream 기반 과거 채팅과 실시간 채팅을 수신합니다.
			""" )
	@ApiResponse( responseCode = "200", description = "채팅방 입장에 성공하였습니다." )
	@ApiResponse( responseCode = "403", description = "로그인이 필요합니다." )
	@ApiResponse( responseCode = "403", description = "채팅 가능한 기간이 아닙니다." )
	@ApiResponse( responseCode = "404", description = "콘서트가 존재하지 않습니다." )
	@PostMapping("/concert/{concertId}/join")
	public RsData<Void> joinChatRoom(@PathVariable Long concertId) {

		chatRoomService.joinChatRoom(concertId);
		return RsData.success(null);
	}

}
