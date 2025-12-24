package com.back.web7_9_codecrete_be.domain.location.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

@Data
public class KakaoRouteTransitRequest {        //Kakao mobility api에서 경유지 추가 response

    private Origin origin;
    private Destination destination;
    private List<Waypoint> waypoints;

    private String priority;
    private String car_fuel;
    private boolean car_hipass;
    private boolean alternatives;
    private boolean road_details;
    private boolean summary;

    @Data
    public static class Origin {
        private double x;
        private double y;
        private int angle;
    }

    @Data
    public static class Destination {
        private double x;
        private double y;
    }

    @Data
    public static class Waypoint {
        private String name;
        private double x;
        private double y;
    }

}
