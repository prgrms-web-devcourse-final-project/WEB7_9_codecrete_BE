package com.back.web7_9_codecrete_be.domain.serverTime.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ServerTimeResponse(

	@Schema(
		description = "티켓 사이트 제공자 이름",
		example = "NOL"
	)
	String provider,

	@Schema(
		description = """
		외부 티켓 서버 시간 - 우리 서버 시간 (밀리초 단위)

		예)
		- offsetMillis = -500
		  → 티켓 서버 시간이 우리 서버보다 0.5초 느림
		- offsetMillis = 1200
		  → 티켓 서버 시간이 우리 서버보다 1.2초 빠름
		""",
		example = "-691"
	)
	long offsetMillis
) {}
