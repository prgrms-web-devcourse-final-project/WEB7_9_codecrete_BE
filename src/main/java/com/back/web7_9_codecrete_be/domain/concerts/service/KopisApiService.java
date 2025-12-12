package com.back.web7_9_codecrete_be.domain.concerts.service;

import com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concert.*;
import com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concertPlace.ConcertPlaceDetailElement;
import com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concertPlace.ConcertPlaceDetailResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concertPlace.ConcertPlaceListElement;
import com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concertPlace.ConcertPlaceListResponse;
import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import com.back.web7_9_codecrete_be.domain.concerts.entity.ConcertPlace;
import com.back.web7_9_codecrete_be.domain.concerts.entity.TicketOffice;
import com.back.web7_9_codecrete_be.domain.concerts.repository.ConcertPlaceRepository;
import com.back.web7_9_codecrete_be.domain.concerts.repository.ConcertRepository;
import com.back.web7_9_codecrete_be.domain.concerts.repository.TicketOfficeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@EnableScheduling
public class KopisApiService {
    // 공연예술통합 전산망 조회를 위한 서비스 클래스입니다.
    private final ConcertRepository concertRepository;

    private final ConcertPlaceRepository placeRepository;

    private final TicketOfficeRepository ticketOfficeRepository;

    //TODO : API key 환경변수로 가져오기
    private String serviceKey = "";
    private LocalDate sdate = LocalDate.of(2025, 12, 1);
    private LocalDate edate = LocalDate.now().plusMonths(6);

    private final RestClient restClient;

    public KopisApiService(ConcertRepository concertRepository, ConcertPlaceRepository placeRepository, TicketOfficeRepository ticketOfficeRepository) {
        this.concertRepository = concertRepository;
        this.placeRepository = placeRepository;
        this.ticketOfficeRepository = ticketOfficeRepository;
        this.restClient = RestClient.builder()
                .baseUrl("https://kopis.or.kr/openApi/restful")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                .build();
    }

    // 데이터 조회 범위를 어떻게 할지? -> 저장은 어떻게 할지? V
    // afterdate 옵션 잘 사용하기
    // 몇일 단위로 patch 하지?
    // 스프링 스케줄러 이용 조회 자동화
    // 인기 장르 인기 공연 상위 노출 -> 조회수? / 평점순?
    // 데이터의 범위 정하기가 까다롭구만..
    // 콘서트 장 위치가 없을 경우 가져와서 저장하기 O
    public ConcertListResponse setConcertsList() throws InterruptedException {


        ConcertListResponse plr;
        List<ConcertListElement> totalConcertsList = new ArrayList<>();
        // 저장시 캐시로 사용할 맵(어차피 400개 정도니까 맵 쓰는게 더 효율적으로 판단)
        Map<String, ConcertPlace> concertPlaceMap = new HashMap<>();

        int page = 1;
        while (true) {
            plr = getConcertListResponse(serviceKey, sdate, edate, page);
            page++;
            if (plr.getConcertList() == null) break;
            for (ConcertListElement p : plr.getConcertList()) {
                totalConcertsList.add(p);
            }
            Thread.sleep(200);
        }

        log.info("Total concert list size: {}", totalConcertsList.size());
        log.info("공연 목록 로드 완료, 공연 세부 내용 로드 및 저장");
        for (ConcertListElement performanceListElement : totalConcertsList) {
            ConcertDetailResponse concertDetailResponse = getConcertDetailResponse(serviceKey, performanceListElement.getApiConcertId());
            ConcertDetailElement concertDetail = concertDetailResponse.getConcertDetail();
            log.info("concert detail: " + concertDetailResponse.getConcertDetail());

            // 콘서트 위치 저장 -> 추후 메소드 추출하기?
            String concertPlaceAPiKey = concertDetailResponse.getConcertDetail().getMt10id();
            ConcertPlace concertPlace;
            concertPlace = concertPlaceMap.getOrDefault(concertPlaceAPiKey, placeRepository.getConcertPlaceByApiConcertPlaceId(concertPlaceAPiKey));
            if (concertPlace == null) {
                // 콘서트 장소가 null일시
                ConcertPlaceDetailResponse concertPlaceDetailElement = getConcertPlaceDetailResponse(serviceKey, concertPlaceAPiKey);
                ConcertPlaceDetailElement concertPlaceDetail = concertPlaceDetailElement.getConcertPlaceDetail();
                concertPlace = concertPlaceDetail.getConcertPlace();
                ConcertPlace savedConcertPlace = placeRepository.save(concertPlace);
                concertPlaceMap.put(concertPlaceAPiKey, savedConcertPlace);
                log.info("concert place saved: " + savedConcertPlace);
            }

            //콘서트 최고 금액, 최저 금액 처리.
            String price = concertDetail.getConcertPrice();
            String[] bits = price.split(" ");
            int maxPrice = 0;
            int minPrice = Integer.MAX_VALUE;
            if (bits.length == 1) {
                minPrice = 0;
            } else {
                for (String bit : bits) {
                    bit = bit.replaceAll(",", "");
                    if (bit.endsWith("원")) {
                        bit = bit.replaceAll("원", "");
                        maxPrice = Math.max(Integer.parseInt(bit), maxPrice);
                        minPrice = Math.min(Integer.parseInt(bit), minPrice);
                    }
                }
            }

            // 콘서트 저장
            Concert concert = new Concert(
                    concertPlace,
                    concertDetail.getConcertName(),
                    concertDetail.getConcertDescription(),
                    "",
                    maxPrice,
                    minPrice,
                    concertDetail.getApiConcertId()
            );

            Concert savedConcert = concertRepository.save(concert);

            List<TicketOfficeResponse> ticketOfficeResponses = concertDetail.getTicketOfficeResponses();

            for (TicketOfficeResponse ticketOffice : ticketOfficeResponses) {
                TicketOffice to = new TicketOffice(
                        savedConcert,
                        ticketOffice.getTicketOfficeName(),
                        ticketOffice.getTicketOfficeUrl()
                );
                ticketOfficeRepository.save(to);
            }
            log.info("Concert saved: " + savedConcert);

            Thread.sleep(300);
        }
        log.info(totalConcertsList.size() + "개의 공연 데이터 저장 완료!");
        return plr;
    }

    @Scheduled(cron = "0 0 2 * * Mon")
    public void updateConcertData() throws InterruptedException {  // 1주일 단위로 추가된 공연을 더하기
        LocalDate weekBefore = LocalDate.now().minusDays(8);
        LocalDate sdate = LocalDate.now().withDayOfMonth(1);
        LocalDate edate = LocalDate.now().withDayOfMonth(1).plusMonths(6);

        ConcertListResponse plr;
        List<ConcertListElement> totalConcertsList = new ArrayList<>();
        // 저장시 캐시로 사용할 맵(어차피 400개 정도니까 맵 쓰는게 더 효율적으로 판단)
        Map<String, ConcertPlace> concertPlaceMap = new HashMap<>();

        int page = 1;
        while (true) {
            plr = getConcertListResponse(serviceKey, sdate, edate, page);
            page++;
            if (plr.getConcertList() == null) break;
            for (ConcertListElement p : plr.getConcertList()) {
                totalConcertsList.add(p);
            }
            Thread.sleep(200);
        }
        log.info("공연 목록 로드 완료, 공연 세부 내용 로드 및 저장");
        for (ConcertListElement performanceListElement : totalConcertsList) {
            ConcertDetailResponse concertDetailResponse = getConcertDetailResponse(serviceKey, performanceListElement.getApiConcertId());
            ConcertDetailElement concertDetail = concertDetailResponse.getConcertDetail();
            log.info("concert detail: " + concertDetailResponse.getConcertDetail());

            // 콘서트 위치 저장 -> 추후 메소드 추출하기?
            String concertPlaceAPiKey = concertDetailResponse.getConcertDetail().getMt10id();
            ConcertPlace concertPlace;
            concertPlace = concertPlaceMap.getOrDefault(concertPlaceAPiKey, placeRepository.getConcertPlaceByApiConcertPlaceId(concertPlaceAPiKey));
            if (concertPlace == null) {
                // 콘서트 장소가 null일시
                ConcertPlaceDetailResponse concertPlaceDetailElement = getConcertPlaceDetailResponse(serviceKey, concertPlaceAPiKey);
                ConcertPlaceDetailElement concertPlaceDetail = concertPlaceDetailElement.getConcertPlaceDetail();
                concertPlace = concertPlaceDetail.getConcertPlace();
                ConcertPlace savedConcertPlace = placeRepository.save(concertPlace);
                concertPlaceMap.put(concertPlaceAPiKey, savedConcertPlace);
                log.info("concert place saved: " + savedConcertPlace);
            }

            //콘서트 최고 금액, 최저 금액 처리.
            String price = concertDetail.getConcertPrice();
            String[] bits = price.split(" ");
            int maxPrice = 0;
            int minPrice = Integer.MAX_VALUE;
            if (bits.length == 1) {
                minPrice = 0;
            } else {
                for (String bit : bits) {
                    bit = bit.replaceAll(",", "");
                    if (bit.endsWith("원")) {
                        bit = bit.replaceAll("원", "");
                        maxPrice = Math.max(Integer.parseInt(bit), maxPrice);
                        minPrice = Math.min(Integer.parseInt(bit), minPrice);
                    }
                }
            }

            // 콘서트 저장
            Concert concert = concertRepository.getConcertByApiConcertId(concertDetail.getApiConcertId());
            if(concert == null){
                concert = new Concert(
                        concertPlace,
                        concertDetail.getConcertName(),
                        concertDetail.getConcertDescription(),
                        "",
                        maxPrice,
                        minPrice,
                        concertDetail.getApiConcertId()
                );
            } else {
                concert = concert.update(
                        concertPlace,
                        concertDetail.getConcertDescription(),
                        "",
                        maxPrice,
                        minPrice
                );
            }

            Concert savedConcert = concertRepository.save(concert);

            List<TicketOfficeResponse> ticketOfficeResponses = concertDetail.getTicketOfficeResponses();

            for (TicketOfficeResponse ticketOffice : ticketOfficeResponses) {
                TicketOffice to = new TicketOffice(
                        savedConcert,
                        ticketOffice.getTicketOfficeName(),
                        ticketOffice.getTicketOfficeUrl()
                );
                ticketOfficeRepository.save(to);
            }
            log.info("Concert saved: " + savedConcert);

            Thread.sleep(300);
        }

    }

    public ConcertListResponse getConcertsList() {
        return getConcertListResponse(serviceKey, sdate, edate, 1);
    }

    public ConcertPlaceListResponse setConcertPlace() throws InterruptedException {
        ConcertPlaceListResponse pplr;
        List<ConcertPlaceListElement> totalConcertsPlaceList = new ArrayList<>();

        if (placeRepository.count() > 1) return null;

        int page = 1;
        while (true) {
            pplr = getConcertPlaceListResponse(serviceKey, page);
            if (pplr.getPerformancePlaceList() == null) break;
            for (ConcertPlaceListElement performancePlaceListElement : pplr.getPerformancePlaceList()) {
                totalConcertsPlaceList.add(performancePlaceListElement);
            }
            page++;
            log.info(page + "번 페이지 탐색 중...");
            Thread.sleep(200);
        }

        log.info("공연장 목록 탐색 완료, 개별 공연장 상세 정보 확인 및 데이터베이스 저장");
        for (ConcertPlaceListElement p : totalConcertsPlaceList) {
            ConcertPlaceDetailResponse performancePlaceDetailResponse = getConcertPlaceDetailResponse(serviceKey, p.getConcertPlaceApiId());
            ConcertPlaceDetailElement concertPlaceDetail = performancePlaceDetailResponse.getConcertPlaceDetail();

            ConcertPlace concertPlace = concertPlaceDetail.getConcertPlace();

            ConcertPlace savedConcertPlace = placeRepository.save(concertPlace);
            log.info("Saved concert place: " + savedConcertPlace);
            Thread.sleep(200);
        }
        log.info("Total concert place list size: " + totalConcertsPlaceList.size());
        return pplr;
    }

    private ConcertListResponse getConcertListResponse(String serviceKey, LocalDate sdate, LocalDate edate, int page) {
        ConcertListResponse performanceListResponse;
        performanceListResponse = restClient.get()
                .uri(uriBuilder ->
                        uriBuilder.path("/pblprfr")
                                .queryParam("service", serviceKey)
                                .queryParam("stdate", sdate.format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                                .queryParam("eddate", edate.format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                                .queryParam("cpage", page)
                                .queryParam("rows", 100)
                                .queryParam("shcate", "CCCD")
                                .queryParam("kidstate", "N")
                                .build()
                ).retrieve().body(ConcertListResponse.class);
        return performanceListResponse;
    }

    private ConcertListResponse getConcertListResponse(String serviceKey, LocalDate sdate, LocalDate edate, int page, LocalDate afterDate) {
        ConcertListResponse performanceListResponse;
        performanceListResponse = restClient.get()
                .uri(uriBuilder ->
                        uriBuilder.path("/pblprfr")
                                .queryParam("service", serviceKey)
                                .queryParam("stdate", sdate.format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                                .queryParam("eddate", edate.format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                                .queryParam("cpage", page)
                                .queryParam("rows", 100)
                                .queryParam("shcate", "CCCD")
                                .queryParam("kidstate", "N")
                                .queryParam("afterdate", afterDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                                .build()
                ).retrieve().body(ConcertListResponse.class);
        return performanceListResponse;
    }


    private ConcertDetailResponse getConcertDetailResponse(String serviceKey, String concertApiId) {
        ConcertDetailResponse concertDetailResponse;
        concertDetailResponse = restClient.get()
                .uri(uriBuilder ->
                        uriBuilder.path("/pblprfr/" + concertApiId)
                                .queryParam("service", serviceKey)
                                .build()
                ).retrieve().body(ConcertDetailResponse.class);

        return concertDetailResponse;
    }


    private ConcertPlaceListResponse getConcertPlaceListResponse(String serviceKey, int page) {
        return restClient.get()
                .uri(uriBuilder ->
                        uriBuilder.path("/prfplc")
                                .queryParam("service", serviceKey)
                                .queryParam("cpage", page)
                                .queryParam("rows", 100)
                                .build()
                ).retrieve().body(ConcertPlaceListResponse.class);
    }

    private ConcertPlaceDetailResponse getConcertPlaceDetailResponse(String serviceKey, String concertPlaceId) {
        return restClient.get()
                .uri(uriBuilder ->
                        uriBuilder.path("/prfplc/" + concertPlaceId)
                                .queryParam("service", serviceKey)
                                .build()
                ).retrieve().body(ConcertPlaceDetailResponse.class);
    }
}
