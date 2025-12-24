package com.back.web7_9_codecrete_be.domain.location.dto.response.kakao;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class KakaoRouteTransitResponse {

    private String transId;
    private List<Route> routes;

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Route {
        private int resultCode;
        private String resultMsg;
        private Summary summary;
        private List<Section> sections;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Summary {
        private Origin origin;
        private Destination destination;
        private List<Waypoint> waypoints;
        private String priority;
        private Bound bound;   // optional일 수 있음
        private Fare fare;
        private int distance;
        private int duration;
    }

    @Data
    public static class Origin {
        private String name;
        private double x;
        private double y;
    }

    @Data
    public static class Destination {
        private String name;
        private double x;
        private double y;
    }

    @Data
    public static class Waypoint {
        private String name;
        private double x;
        private double y;
    }

    @Data
    public static class Bound {
        private double minX;
        private double minY;
        private double maxX;
        private double maxY;
    }

    @Data
    public static class Fare {
        private int taxi;
        private int toll;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Section {
        private int distance;
        private int duration;
        private Bound bound;        // summary=false일 때만 올 수 있음
        private List<Road> roads;   // summary=false일 때만 올 수 있음
        private List<Guide> guides; // summary=false일 때만 올 수 있음
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Road {
        private String name;
        private int distance;
        private int duration;
        private double trafficSpeed;
        private int trafficState;
        private List<Double> vertexes;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Guide {
        private String name;
        private double x;
        private double y;
        private int distance;
        private int duration;
        private int type;
        private String guidance;
        private int roadIndex;
    }
}
