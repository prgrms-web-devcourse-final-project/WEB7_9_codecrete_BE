package com.back.web7_9_codecrete_be.domain.location.dto;

import lombok.Data;

@Data
public class TmapResponse {

    private String startX;
    private String startY;
    private String endX;
    private String endY;
    private int count;          //최대 응답 결과 개수
    private String format;      //출력포멧 : jsom, xml

}
