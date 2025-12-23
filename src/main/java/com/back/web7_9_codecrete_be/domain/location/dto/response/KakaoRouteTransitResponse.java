package com.back.web7_9_codecrete_be.domain.location.dto.response;

import lombok.Getter;
import java.util.List;

@Getter
public class KakaoRouteTransitResponse {

    private List<Route> routes;

    @Getter
    public static class Route {
        private Summary summary;
        private List<Section> sections;
    }

    @Getter
    public static class Summary {
        private Origin origin;
        private Destination destination;
        private List<Waypoint> waypoints; // ✅ 경유지(요청값)

        private String priority;
        private Fare fare;

        private int distance; // meters
        private int duration; // seconds
    }

    @Getter
    public static class Origin {
        private String name;
        private double x;
        private double y;
    }

    @Getter
    public static class Destination {
        private String name;
        private double x;
        private double y;
    }

    @Getter
    public static class Waypoint {
        private String name;
        private double x;
        private double y;
    }

    @Getter
    public static class Fare {
        private int taxi;
        private int toll;
    }

    @Getter
    public static class Section {
        private List<Road> roads;
        private List<Guide> guides;
    }

    @Getter
    public static class Road {
        private List<Double> vertexes;
    }

    @Getter
    public static class Guide {
        private String name;
        private double x;
        private double y;
        private int distance;
        private int duration;
        private int type;
        private String guidance;
        private int road_index;
    }
}
