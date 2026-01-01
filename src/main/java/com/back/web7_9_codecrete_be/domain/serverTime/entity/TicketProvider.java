package com.back.web7_9_codecrete_be.domain.serverTime.entity;

public enum TicketProvider {

	NOL("https://nol.interpark.com/ticket"),
	YES24("https://ticket.yes24.com"),
	MELON("https://ticket.melon.com"),
	TICKETLINK("https://www.ticketlink.co.kr");

	private final String url;

	TicketProvider(String url) {
		this.url = url;
	}

	public String getUrl() {
		return url;
	}
}
