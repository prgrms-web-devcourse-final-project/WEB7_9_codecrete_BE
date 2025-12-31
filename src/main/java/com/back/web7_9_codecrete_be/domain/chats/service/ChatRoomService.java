package com.back.web7_9_codecrete_be.domain.chats.service;

import java.time.Duration;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.web7_9_codecrete_be.domain.chats.entity.ChatRoom;
import com.back.web7_9_codecrete_be.domain.chats.repository.ChatRoomRepository;
import com.back.web7_9_codecrete_be.domain.chats.repository.ChatStreamRepository;
import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import com.back.web7_9_codecrete_be.domain.concerts.repository.ConcertRepository;
import com.back.web7_9_codecrete_be.global.error.code.ChatErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatPolicyService chatPolicyService;
	private final ConcertRepository concertRepository;
	private final ChatStreamRepository chatStreamRepository;

	/**
	 * 채팅방 입장
	 * - 정책 기간 검증 (Redis 캐시 우선)
	 * - 채팅방 Lazy 생성
	 */
	public void joinChatRoom(Long concertId) {

		// 채팅 가능 정책 검증
		if (!chatPolicyService.isChatAvailable(concertId)) {
			throw new BusinessException(ChatErrorCode.CHAT_NOT_AVAILABLE);
		}

		// 채팅방 조회 or Lazy 생성
		chatRoomRepository.findByConcert_ConcertId(concertId)
			.orElseGet(() -> createChatRoomSafely(concertId));

		// Redis Stream TTL 설정
		if (!chatStreamRepository.hasTtl(concertId)) {
			Duration ttl = chatPolicyService.calculateChatRemainingTtl(concertId);

			if (!ttl.isZero() && !ttl.isNegative()) {
				chatStreamRepository.setTtl(concertId, ttl);
			}
		}

	}

	/**
	 * 채팅방 Lazy 생성
	 * - DB UNIQUE(concert_id)로 동시성 처리
	 */
	private ChatRoom createChatRoomSafely(Long concertId) {

		try {
			Concert concert = concertRepository.getReferenceById(concertId);

			return chatRoomRepository.save(ChatRoom.create(concert));

		} catch (DataIntegrityViolationException e) {
			// 동시에 다른 요청이 먼저 생성한 경우
			return chatRoomRepository.findByConcert_ConcertId(concertId)
				.orElseThrow(() ->
					new IllegalStateException("채팅방 생성 충돌 후 재조회 실패"));
		}
	}
}
