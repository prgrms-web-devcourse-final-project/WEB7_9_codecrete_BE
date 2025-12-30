package com.back.web7_9_codecrete_be.domain.chats.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.back.web7_9_codecrete_be.domain.chats.dto.response.ChatReadResponse;
import com.back.web7_9_codecrete_be.domain.chats.service.ChatMessageReadService;
import com.back.web7_9_codecrete_be.global.rsData.RsData;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Chat", description = "채팅 관련 API")
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatMessageReadController {

	private final ChatMessageReadService chatMessageReadService;

	@Operation(
		summary = "채팅 메시지 조회 (cursor 기반 무한 스크롤)",
		description = """
			채팅 메시지를 cursor(before) 기준으로 조회합니다.

			- before 미전달 시: 최신 메시지를 조회합니다.
			- before 전달 시: 해당 cursor 이전의 메시지를 조회합니다.
			"""
	)
	@GetMapping("/{concertId}/messages")
	public RsData<List<ChatReadResponse>> getMessages(
		@Parameter(
			description = "공연 ID",
			example = "1",
			required = true
		)
		@PathVariable Long concertId,

		@Parameter(
			description = """
				조회 기준 cursor (Redis Stream ID).
				해당 값이 주어지면, 그 이전의 채팅 메시지를 조회합니다.
				""",
			example = "1734940012345-0",
			required = false
		)
		@RequestParam(required = false) String before,

		@Parameter(
			description = "한 번에 조회할 메시지 개수 (기본값: 20)",
			example = "20",
			required = false
		)
		@RequestParam(required = false) Integer size
	) {
		return RsData.success(
			chatMessageReadService.getMessages(concertId, before, size)
		);
	}
}
