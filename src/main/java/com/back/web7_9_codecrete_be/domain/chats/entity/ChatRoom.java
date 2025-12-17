package com.back.web7_9_codecrete_be.domain.chats.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "chat_room_id")
	private Long id;

	@CreatedDate
	@Column(name = "created_date", nullable = false)
	private LocalDateTime createdDate;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "concert_id", nullable = false, unique = true)
	private Concert concert;

	private ChatRoom(Concert concert) {
		this.concert = concert;
	}

	public static ChatRoom create(Concert concert) {
		return new ChatRoom(concert);
	}
}
