package com.back.web7_9_codecrete_be.domain.location.dto;

import lombok.Data;

import java.util.List;
@Data
public class KakaoCoordinateResponse {              //좌표 -> 주소로 바꾸는 response

    private List<Document> documents;

    @Data
    public static class Document {
        private Address address;
        private RoadAddress road_address;
    }

    @Data
    public static class Address {
        private String address_name;
        private String region_1depth_name;
        private String region_2depth_name;
        private String region_3depth_name;
        private String mountain_yn;
        private String main_address_no;
        private String sub_address_no;
    }

    @Data
    public static class RoadAddress {
        private String address_name;
        private String road_name;
        private String building_name;
        private String zone_no;
    }
}
