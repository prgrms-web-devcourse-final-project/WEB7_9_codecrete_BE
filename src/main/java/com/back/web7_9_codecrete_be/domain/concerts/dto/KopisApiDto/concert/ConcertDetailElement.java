package com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concert;


import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ConcertDetailElement {
    @JacksonXmlProperty(localName = "mt20id")
    private String apiConcertId;

    @JacksonXmlProperty(localName = "prfnm")
    private String concertName;

    @JacksonXmlProperty(localName = "prfpdfrom")
    private String startDate;

    @JacksonXmlProperty(localName = "prfpdto")
    private String endDate;

    @JacksonXmlProperty(localName = "fcltynm")
    private String concertPlaceName;

    @JacksonXmlProperty(localName = "prfcast")
    private String concertCast;

    @JacksonXmlProperty(localName = "prfcrew")
    private String concertCrew;

    @JacksonXmlProperty(localName = "prfruntime")
    private String concertDuration;

    @JacksonXmlProperty(localName = "prfage")
    private String concertAge;

    @JacksonXmlProperty(localName = "entrpsnm")
    private String concertTotalCompany;

    @JacksonXmlProperty(localName = "entrpsnmP")
    private String concertProductionCompany;

    @JacksonXmlProperty(localName = "entrpsnmA")
    private String concertPlanningCompany;

    @JacksonXmlProperty(localName = "entrpsnmH")
    private String concertHost;

    @JacksonXmlProperty(localName = "entrpsnmS")
    private String concertOrganizer;

    @JacksonXmlProperty(localName = "pcseguidance")
    private String concertPrice;

    @JacksonXmlProperty(localName = "poster")
    private String posterUrl;

    @JacksonXmlProperty(localName = "sty")
    private String concertDescription;

    @JacksonXmlProperty(localName = "area")
    private String area;

    @JacksonXmlProperty(localName = "genrenm")
    private String genreName;

    @JacksonXmlProperty(localName = "openrun")
    private String openrun;

    @JacksonXmlProperty(localName = "visit")
    private String visit;

    @JacksonXmlProperty(localName = "child")
    private String child;

    @JacksonXmlProperty(localName = "daehakro")
    private String daehakro;

    @JacksonXmlProperty(localName = "festival")
    private String festival;

    @JacksonXmlProperty(localName = "musicallicense")
    private String musicalLicense;

    @JacksonXmlProperty(localName = "musicalcreate")
    private String musicalCreate;

    @JacksonXmlProperty(localName = "updatedate")
    private String updateDate;

    @JacksonXmlProperty(localName = "prfstate")
    private String concertState;

    // -----------------------
    // styurls: <styurls><styurl>...</styurl>...</styurls>
    // -----------------------
    @JacksonXmlProperty(localName = "styurl")
    @JacksonXmlElementWrapper(localName = "styurls")
    private List<String> concertImageUrls;

    // -----------------------
    // relates: <relates><relate>...</relate>...</relates>
    // -----------------------
    @JacksonXmlProperty(localName = "relate")
    @JacksonXmlElementWrapper(localName = "relates")
    private List<TicketOfficeResponse> ticketOfficeResponses;

    @JacksonXmlProperty(localName = "mt10id")
    private String mt10id;

    @JacksonXmlProperty(localName = "dtguidance")
    private String concertTime;

    @Override
    public String toString() {
        return "PerformanceDetailElement{" +
                "mt20id='" + apiConcertId + '\'' +
                ", concertName='" + concertName + '\'' +
                ", startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", concertPlaceName='" + concertPlaceName + '\'' +
                ", concertCast='" + concertCast + '\'' +
                ", concertCrew='" + concertCrew + '\'' +
                ", concertDuration='" + concertDuration + '\'' +
                ", concertAge='" + concertAge + '\'' +
                ", concertTotalCompany='" + concertTotalCompany + '\'' +
                ", concertProductionCompany='" + concertProductionCompany + '\'' +
                ", concertPlanningCompany='" + concertPlanningCompany + '\'' +
                ", concertHost='" + concertHost + '\'' +
                ", concertOrganizer='" + concertOrganizer + '\'' +
                ", concertPrice='" + concertPrice + '\'' +
                ", posterUrl='" + posterUrl + '\'' +
                ", concertDescription='" + concertDescription + '\'' +
                ", area='" + area + '\'' +
                ", genreName='" + genreName + '\'' +
                ", openrun='" + openrun + '\'' +
                ", visit='" + visit + '\'' +
                ", child='" + child + '\'' +
                ", daehakro='" + daehakro + '\'' +
                ", festival='" + festival + '\'' +
                ", musicalLicense='" + musicalLicense + '\'' +
                ", musicalCreate='" + musicalCreate + '\'' +
                ", updateDate='" + updateDate + '\'' +
                ", concertState='" + concertState + '\'' +
                ", concertImageUrls=" + concertImageUrls +
                ", ticketOffices=" + ticketOfficeResponses +
                ", mt10id='" + mt10id + '\'' +
                ", concertTime='" + concertTime + '\'' +
                '}';
    }


}
