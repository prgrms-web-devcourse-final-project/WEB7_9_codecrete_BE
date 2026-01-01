package com.back.web7_9_codecrete_be.domain.serverTime.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.web7_9_codecrete_be.domain.serverTime.dto.ServerTimeResponse;
import com.back.web7_9_codecrete_be.domain.serverTime.entity.TicketProvider;
import com.back.web7_9_codecrete_be.domain.serverTime.service.TicketServerTimeService;
import com.back.web7_9_codecrete_be.global.rsData.RsData;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(
	name = "Server Time",
	description = """
	외부 티켓 사이트의 서버 시간과
	우리 서버 시간 간의 **차이(offset)** 를 제공하는 API

	지원 티켓사(provider):
	- NOL
	- YES24
	- MELON
	- TICKETLINK
	"""
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/server-time")
public class TicketServerTimeController {

	private final TicketServerTimeService ticketServerTimeService;

	@Operation(
		summary = "티켓 서버 시간 offset 조회",
		description = """
		외부 티켓 사이트의 HTTP Date 헤더를 기반으로
		우리 서버와의 **시간 차이(offsetMillis)** 를 **밀리초** 단위로 계산해 반환합니다.

		- offsetMillis는 60초간 캐싱됩니다.
		"""
	)
	@GetMapping("/{provider}")
	public RsData<ServerTimeResponse> getServerTime(
		@Parameter(
			description = "티켓 사이트 제공자",
			example = "NOL",
			required = true
		)
		@PathVariable String provider
	) {
		TicketProvider ticketProvider = TicketProvider.valueOf(provider.toUpperCase());

		ServerTimeResponse response = ticketServerTimeService.fetchServerTime(ticketProvider);

		return RsData.success(response);
	}
}
