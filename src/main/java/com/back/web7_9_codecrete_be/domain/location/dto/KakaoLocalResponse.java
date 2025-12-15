package com.back.web7_9_codecrete_be.domain.location.dto;

import lombok.Data;

import java.util.List;

@Data
public class KakaoLocalResponse {
    private List<Document> documents;

    @Data
    public static class Document {
        private String place_name;
        private String x; // longitude
        private String y; // latitude
        private String road_address_name;
        private String address_name;
        private String place_url;
    }
}