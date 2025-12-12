package com.back.web7_9_codecrete_be.domain.chats.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.web7_9_codecrete_be.domain.chats.service.ChatRoomService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

	private final ChatRoomService chatRoomService;

}
