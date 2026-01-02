package com.back.web7_9_codecrete_be.domain.chats.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatUserCache {

	private Long userId;
	private String nickname;
	private String profileImage;
}
