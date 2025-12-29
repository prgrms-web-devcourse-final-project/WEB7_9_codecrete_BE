package com.back.web7_9_codecrete_be.domain.chats.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.back.web7_9_codecrete_be.domain.chats.dto.response.ChatReadResponse;
import com.back.web7_9_codecrete_be.domain.chats.repository.ChatStreamRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatMessageReadService {

	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 50;

	private final ChatStreamRepository chatStreamRepository;

	public List<ChatReadResponse> getMessages(
		Long concertId,
		String before,
		Integer size
	) {
		int limit = Math.min(
			size != null ? size : DEFAULT_SIZE,
			MAX_SIZE
		);

		// 최초 진입
		if (before == null || before.isBlank()) {
			return chatStreamRepository.findLatest(concertId, limit);
		}

		// 이전 메시지 조회 (무한 스크롤)
		return chatStreamRepository.findBefore(concertId, before, limit);
	}
}
