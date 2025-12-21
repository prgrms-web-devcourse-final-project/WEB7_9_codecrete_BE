package com.back.web7_9_codecrete_be.domain.location.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
@Getter
@AllArgsConstructor
public class KakaoMobilityResponse {


    private List<Route> routes;

    @Getter
    public static class Route {
        private Summary summary;
        private List<Section> sections;
    }

    @Getter
    public static class Summary {
        private int distance;   // meters
        private int duration;   // seconds
    }

    @Getter
    public static class Section{
        private List<Road> roads;
        private List<Guide> guides;
        public List<Guide> getGuides() {
            return guides;
        }
    }

    @Getter
    public static class Road{
        private List<Double> vertexes;
    }


    @Getter
    public static class Guide {   // ✅ 추가
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
