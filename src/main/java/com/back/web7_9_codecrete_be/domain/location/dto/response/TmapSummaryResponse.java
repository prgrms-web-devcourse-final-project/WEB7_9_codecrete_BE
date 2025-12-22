package com.back.web7_9_codecrete_be.domain.location.dto.response;

import lombok.Getter;

import java.util.List;

@Getter
public class TmapSummaryResponse {

        private MetaData metaData;

        @Getter
        public static class MetaData {
            private Plan plan;
        }

        @Getter
        public static class Plan {
            private List<Itinerary> itineraries;
        }

        @Getter
        public static class Itinerary {
            private int totalTime;
            private int totalDistance;
            private int transferCount;
            private Fare fare;
        }

        @Getter
        public static class Fare {
            private Regular regular;
        }

        @Getter
        public static class Regular {
            private int totalFare;
        }

}
