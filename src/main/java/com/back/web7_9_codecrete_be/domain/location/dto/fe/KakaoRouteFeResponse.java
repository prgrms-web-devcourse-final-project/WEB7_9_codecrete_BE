package com.back.web7_9_codecrete_be.domain.location.dto.fe;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "프론트 전달용 구간 경로 정보 (경유지 인덱스/거리/시간/좌표)")
public class KakaoRouteFeResponse {

    @Schema(description = "구간 인덱스 (경유지 기준)", example = "0")
    private int routeIndex;

    @Schema(description = "구간 거리 (meter)", example = "1200")
    private int distance;

    @Schema(description = "구간 소요 시간 (second)", example = "540")
    private int duration;

    @Schema(description = "출발 좌표/지점 정보 (권장: DTO로 변경)", example = "{\"x\":126.977969,\"y\":37.566535}")
    private Object from;

    @Schema(description = "도착 좌표/지점 정보 (권장: DTO로 변경)", example = "{\"x\":126.986037,\"y\":37.563617}")
    private Object to;
}
