package com.back.web7_9_codecrete_be.domain.location.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KakaoRouteFeResponse {             //경유지까지의 인덱스, 거리, 시간, 좌표를 나타냄
    private int routeIndex;
    private int distance;
    private int duration;
    private Object from;
    private Object to;

}
