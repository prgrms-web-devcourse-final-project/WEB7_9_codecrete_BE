package com.back.web7_9_codecrete_be.domain.plans.dto.request;

import com.back.web7_9_codecrete_be.domain.plans.entity.Schedule;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@NoArgsConstructor
public class ScheduleUpdateRequest {

    // 부분 업데이트 지원: 모든 필드를 optional로 변경
    private Schedule.ScheduleType scheduleType;

    @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
    private String title;

    private LocalTime startAt;

    @Positive(message = "소요 시간은 양수여야 합니다.")
    private Integer duration;

    @Size(max = 255, message = "위치는 255자 이하여야 합니다.")
    private String location;

    @Min(value = -90, message = "위도는 -90 이상이어야 합니다.")
    @Max(value = 90, message = "위도는 90 이하여야 합니다.")
    private Double locationLat;

    @Min(value = -180, message = "경도는 -180 이상이어야 합니다.")
    @Max(value = 180, message = "경도는 180 이하여야 합니다.")
    private Double locationLon;

    @PositiveOrZero(message = "예상 비용은 0 이상이어야 합니다.")
    private Integer estimatedCost;

    private String details;

    // 교통 수단인 경우 사용
    @Min(value = -90, message = "출발지 위도는 -90 이상이어야 합니다.")
    @Max(value = 90, message = "출발지 위도는 90 이하여야 합니다.")
    private Double startPlaceLat;

    @Min(value = -180, message = "출발지 경도는 -180 이상이어야 합니다.")
    @Max(value = 180, message = "출발지 경도는 180 이하여야 합니다.")
    private Double startPlaceLon;

    @Min(value = -90, message = "도착지 위도는 -90 이상이어야 합니다.")
    @Max(value = 90, message = "도착지 위도는 90 이하여야 합니다.")
    private Double endPlaceLat;

    @Min(value = -180, message = "도착지 경도는 -180 이상이어야 합니다.")
    @Max(value = 180, message = "도착지 경도는 180 이하여야 합니다.")
    private Double endPlaceLon;

    @Positive(message = "거리는 양수여야 합니다.")
    private Integer distance;

    private Schedule.TransportType transportType;

    /**
     * 위도와 경도는 함께 제공되어야 함 (둘 다 null이거나 둘 다 값이 있어야 함)
     * 단, TRANSPORT 타입일 때는 locationLat/Lon을 사용하지 않음 (endPlaceLat/Lon 사용)
     * 부분 업데이트 시에도 적용
     */
    @AssertTrue(message = "위도와 경도는 함께 제공되어야 합니다. TRANSPORT 타입일 경우 locationLat/Lon은 사용할 수 없습니다.")
    private boolean isValidLocationCoordinates() {
        // scheduleType이 null이면 기존 타입을 확인할 수 없으므로 통과 (Service 레벨에서 처리)
        if (scheduleType == null) {
            return true;
        }
        // TRANSPORT 타입일 때는 locationLat/Lon을 사용하지 않음
        if (scheduleType == Schedule.ScheduleType.TRANSPORT) {
            return locationLat == null && locationLon == null;
        }
        // 일반 일정: 둘 다 null이거나 둘 다 값이 있어야 함
        return (locationLat == null && locationLon == null) || 
               (locationLat != null && locationLon != null);
    }

    /**
     * TRANSPORT 타입일 때 교통 관련 필드들이 필수인지 검증
     * 부분 업데이트 시에도 TRANSPORT 타입으로 변경하는 경우 필수 필드 검증
     */
    @AssertTrue(message = "교통 수단 타입일 경우 출발지/도착지 좌표, 거리, 교통 수단 종류는 필수입니다.")
    private boolean isValidTransportFields() {
        // scheduleType이 null이거나 TRANSPORT가 아니면 검증 통과
        if (scheduleType == null || scheduleType != Schedule.ScheduleType.TRANSPORT) {
            return true;
        }
        // TRANSPORT 타입일 때 필수 필드 검증 (부분 업데이트 지원)
        return startPlaceLat != null && startPlaceLon != null &&
               endPlaceLat != null && endPlaceLon != null &&
               distance != null && transportType != null;
    }
}
