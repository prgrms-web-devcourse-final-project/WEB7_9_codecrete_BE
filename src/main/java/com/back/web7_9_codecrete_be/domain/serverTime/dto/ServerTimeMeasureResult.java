package com.back.web7_9_codecrete_be.domain.serverTime.dto;

public record ServerTimeMeasureResult(
	long offsetMillis,
	long rttMillis
) {}

