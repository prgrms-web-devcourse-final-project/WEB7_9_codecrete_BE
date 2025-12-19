package com.back.web7_9_codecrete_be.domain.location.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "카카오 로컬 API 장소 검색 응답 DTO")
public class KakaoLocalResponse {

    @Schema(description = "장소 검색 결과 목록")
    private List<Document> documents;

    @Data
    @Schema(description = "카카오 장소 정보")
    public static class Document {

        @Schema(description = "장소명", example = "디라이프 스타일키친 광화문점")
        private String place_name;

        @Schema(description = "경도(longitude)", example = "126.97826128583668")
        private String x;

        @Schema(description = "위도(latitude)", example = "37.56842610180289")
        private String y;

        @Schema(description = "도로명 주소", example = "서울 중구 세종대로 136")
        private String road_address_name;

        @Schema(description = "지번 주소", example = "서울 중구 태평로1가 84")
        private String address_name;

        @Schema(description = "카카오 장소 상세 URL", example = "http://place.map.kakao.com/444516464")
        private String place_url;
    }
}
