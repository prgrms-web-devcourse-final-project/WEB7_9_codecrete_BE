package com.back.web7_9_codecrete_be.domain.chats.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatParticipantResponse {

	private Long userId;
	private String nickname;
	private String profileImage;
}
