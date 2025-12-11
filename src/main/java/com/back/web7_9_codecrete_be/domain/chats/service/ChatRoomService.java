package com.back.web7_9_codecrete_be.domain.chats.service;

import org.springframework.stereotype.Service;

import com.back.web7_9_codecrete_be.domain.chats.repository.ChatRoomRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

	private final ChatRoomRepository chatRoomRepository;
}
