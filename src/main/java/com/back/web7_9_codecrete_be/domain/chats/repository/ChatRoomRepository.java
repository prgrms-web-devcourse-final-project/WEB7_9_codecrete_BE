package com.back.web7_9_codecrete_be.domain.chats.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.web7_9_codecrete_be.domain.chats.entity.ChatRoom;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
}
