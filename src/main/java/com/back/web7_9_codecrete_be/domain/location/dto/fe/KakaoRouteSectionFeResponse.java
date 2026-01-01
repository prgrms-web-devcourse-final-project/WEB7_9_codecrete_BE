package com.back.web7_9_codecrete_be.domain.location.dto.fe;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
@Data
@AllArgsConstructor
public class KakaoRouteSectionFeResponse {          //프론트에서 원하는 전체 거리, 시간, 좌표로, section에서는 경유지를 거치는 값들을 저장
    private int totalDistance;
    private int totalDuration;
    private List<KakaoRouteFeResponse> sections;
}
