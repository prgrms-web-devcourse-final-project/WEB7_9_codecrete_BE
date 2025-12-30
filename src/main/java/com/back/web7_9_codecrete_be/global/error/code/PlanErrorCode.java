package com.back.web7_9_codecrete_be.global.error.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlanErrorCode implements ErrorCode {

	PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "P-100", "계획을 찾을 수 없습니다."),
	PLAN_FORBIDDEN(HttpStatus.FORBIDDEN, "P-101", "해당 계획에 접근할 수 없습니다."),
	SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "P-102", "일정을 찾을 수 없습니다."),
	PLAN_UNAUTHORIZED(HttpStatus.FORBIDDEN, "P-103", "해당 작업을 수행할 권한이 없습니다."),
	SCHEDULE_INVALID_TRANSPORT_FIELDS(HttpStatus.BAD_REQUEST, "P-104", "교통 수단 타입일 경우 출발지/도착지 좌표, 거리, 교통 수단 종류는 필수입니다."),
	SCHEDULE_INVALID_LOCATION_FOR_TRANSPORT(HttpStatus.BAD_REQUEST, "P-105", "교통 수단 타입일 경우 locationLat/Lon은 사용할 수 없습니다. endPlaceLat/Lon을 사용해주세요."),
	CONCERT_NOT_FOUND(HttpStatus.NOT_FOUND, "P-106", "공연을 찾을 수 없습니다."),
	SCHEDULE_INVALID_LOCATION_COORDINATES(HttpStatus.BAD_REQUEST, "P-107", "위도와 경도는 함께 제공되어야 합니다."),
	SCHEDULE_MAIN_EVENT_NOT_DELETABLE(HttpStatus.FORBIDDEN, "P-108", "메인 이벤트(콘서트) 일정은 삭제할 수 없습니다."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "P-109", "사용자를 찾을 수 없습니다."),
	USER_ALREADY_PARTICIPANT(HttpStatus.BAD_REQUEST, "P-110", "이미 참가자로 등록된 사용자입니다."),
	INVALID_SHARE_TOKEN(HttpStatus.NOT_FOUND, "P-111", "유효하지 않은 공유 링크입니다."),
	SHARE_TOKEN_NOT_GENERATED(HttpStatus.BAD_REQUEST, "P-112", "공유 링크가 생성되지 않았습니다."),
	SHARE_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "P-113", "공유 링크가 만료되었습니다."),
	PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "P-114", "참가자를 찾을 수 없습니다."),
	CANNOT_REMOVE_OWNER(HttpStatus.BAD_REQUEST, "P-115", "소유자는 강퇴할 수 없습니다."),
	CANNOT_LEAVE_OWNER(HttpStatus.BAD_REQUEST, "P-116", "소유자는 계획에서 나갈 수 없습니다."),
	INVALID_INVITE_STATUS(HttpStatus.BAD_REQUEST, "P-117", "유효하지 않은 초대 상태입니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}