package com.back.web7_9_codecrete_be.domain.chats.entity;

import java.time.LocalDateTime;

import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;

@Getter
@Entity
public class ChatRoom {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "chat_room_id")
	private long id;

	@Column(nullable = false)
	private LocalDateTime createdDate;

	@Column(nullable = false)
	private LocalDateTime expiresDate;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "concert_id", nullable = false, unique = true)
	private Concert concert;

}
