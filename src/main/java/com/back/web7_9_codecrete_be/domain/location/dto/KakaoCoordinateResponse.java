package com.back.web7_9_codecrete_be.domain.location.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "카카오 좌표 → 주소 변환 응답 DTO")
public class KakaoCoordinateResponse {

    @Schema(
            description = "주소 변환 결과 목록",
            example = "[]"
    )
    private List<Document> documents;

    @Data
    @Schema(description = "좌표 변환 결과 문서")
    public static class Document {

        @Schema(description = "지번 주소 정보")
        private Address address;

        @Schema(description = "도로명 주소 정보")
        private RoadAddress road_address;
    }

    @Data
    @Schema(description = "지번 주소 상세 정보")
    public static class Address {

        @Schema(description = "전체 지번 주소", example = "서울 중구 태평로1가 31")
        private String address_name;

        @Schema(description = "시/도", example = "서울")
        private String region_1depth_name;

        @Schema(description = "구", example = "중구")
        private String region_2depth_name;

        @Schema(description = "동", example = "태평로1가")
        private String region_3depth_name;

        @Schema(description = "산 여부 (Y/N)", example = "N")
        private String mountain_yn;

        @Schema(description = "본번", example = "31")
        private String main_address_no;

        @Schema(description = "부번", example = "0")
        private String sub_address_no;
    }

    @Data
    @Schema(description = "도로명 주소 상세 정보")
    public static class RoadAddress {

        @Schema(description = "전체 도로명 주소", example = "서울 중구 세종대로 110")
        private String address_name;

        @Schema(description = "도로명", example = "세종대로")
        private String road_name;

        @Schema(description = "건물명", example = "서울시청")
        private String building_name;

        @Schema(description = "우편번호", example = "04524")
        private String zone_no;
    }
}
