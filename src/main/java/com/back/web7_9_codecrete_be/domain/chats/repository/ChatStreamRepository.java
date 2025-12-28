package com.back.web7_9_codecrete_be.domain.chats.repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.back.web7_9_codecrete_be.domain.chats.dto.response.ChatMessageResponse;
import com.back.web7_9_codecrete_be.domain.chats.dto.response.ChatReadResponse;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ChatStreamRepository {

	private final RedisTemplate<String, Object> redisTemplate;

	private String streamKey(Long concertId) {
		return "chat:stream:" + concertId;
	}

	/** 저장 */
	public void save(ChatMessageResponse message) {
		String key = streamKey(message.getConcertId());

		Map<String, String> fields = new HashMap<>();
		fields.put("concertId", message.getConcertId().toString());
		fields.put("senderId", message.getSenderId().toString());
		fields.put("senderName", message.getSenderName());
		fields.put("content", message.getContent());
		fields.put("sentDate", message.getSentDate().toString());

		redisTemplate.opsForStream().add(
			StreamRecords.newRecord()
				.in(key)
				.ofMap(fields)
		);
	}

	public boolean hasTtl(Long concertId) {
		Long ttl = redisTemplate.getExpire(streamKey(concertId));
		return ttl != null && ttl >= 0;
	}

	public void setTtl(Long concertId, Duration ttl) {
		redisTemplate.expire(streamKey(concertId), ttl);
	}

	/** 조회 */
	// 채팅방 진입시 가장 최신 메시지 size개 조회
	public List<ChatReadResponse> findLatest(Long concertId, int size) {

		List<MapRecord<String, Object, Object>> records =
			redisTemplate.opsForStream().reverseRange(
				streamKey(concertId),
				Range.unbounded(),
				Limit.limit().count(size)
			);

		return toResponses(records);
	}

	// cursor(before) 기반 과거 메시지 조회
	// beforeMessageId보다 이전에 있던 메시지들 중 최신 size개 조회
	public List<ChatReadResponse> findBefore(
		Long concertId,
		String beforeMessageId,
		int size
	) {
		String exclusiveBeforeId = exclusiveBefore(beforeMessageId);

		List<MapRecord<String, Object, Object>> records =
			redisTemplate.opsForStream().reverseRange(
				streamKey(concertId),
				Range.leftOpen("0-0", exclusiveBeforeId),
				Limit.limit().count(size)
			);

		return toResponses(records);
	}

	private List<ChatReadResponse> toResponses(
		List<MapRecord<String, Object, Object>> records
	) {
		if (records == null || records.isEmpty()) {
			return List.of();
		}

		return records.stream()
			.map(record -> {
				Map<Object, Object> v = record.getValue();

				return new ChatReadResponse(
					record.getId().getValue(),
					Long.valueOf(v.get("concertId").toString()),
					Long.valueOf(v.get("senderId").toString()),
					v.get("senderName").toString(),
					v.get("content").toString(),
					LocalDateTime.parse(v.get("sentDate").toString())
				);
			})
			.filter(Objects::nonNull)
			.toList();
	}

	private String exclusiveBefore(String messageId) {
		String[] parts = messageId.split("-");
		long time = Long.parseLong(parts[0]); // 밀리초 타임스탬프
		long seq = Long.parseLong(parts[1]); // 같은 밀리초 내 순번

		if (seq > 0) {
			return time + "-" + (seq - 1);
		}

		// seq == 0 인 경우, timestamp를 1 감소
		return (time - 1) + "-0";
	}
}
