package com.back.web7_9_codecrete_be.domain.location.dto.fe;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "프론트 전달용 경로 정보 (총 거리/시간 + 구간 정보)")
public class KakaoRouteSectionFeResponse {

    @Schema(description = "전체 거리 (meter)", example = "8500")
    private int totalDistance;

    @Schema(description = "전체 소요 시간 (second)", example = "3200")
    private int totalDuration;

    @Schema(description = "택시비", example = "12000")
    private  int totalTaxi;         //택시비 추가

    @Schema(description = "구간(section) 목록")
    private List<KakaoRouteFeResponse> sections;
}
