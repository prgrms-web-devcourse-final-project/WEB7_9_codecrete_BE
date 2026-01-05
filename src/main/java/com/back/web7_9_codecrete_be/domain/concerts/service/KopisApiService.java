package com.back.web7_9_codecrete_be.domain.concerts.service;

import com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concert.*;
import com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concertPlace.ConcertPlaceDetailElement;
import com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concertPlace.ConcertPlaceDetailResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concertPlace.ConcertPlaceListElement;
import com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concertPlace.ConcertPlaceListResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.result.SetResultResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ListSort;
import com.back.web7_9_codecrete_be.domain.concerts.entity.*;
import com.back.web7_9_codecrete_be.domain.concerts.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@EnableAsync
@EnableRetry
public class KopisApiService {
    // 공연예술통합 전산망 조회를 위한 서비스 클래스입니다.
    private final ConcertRepository concertRepository;

    private final ConcertPlaceRepository placeRepository;

    private final TicketOfficeRepository ticketOfficeRepository;

    private final ConcertImageRepository imageRepository;

    private final ConcertKopisApiLogService concertKopisApiLogService;

    private final ConcertRedisRepository concertRedisRepository;

    private final ArtistRepository artistRepository;

    private final ConcertArtistRepository concertArtistRepository;

    @Value("${kopis.api-key}")
    private String serviceKey;

    private LocalDate sdate = LocalDate.of(2025, 12, 1);
    private LocalDate edate = LocalDate.now().plusYears(1);

    private final RestClient restClient;

    public KopisApiService(
            ConcertRepository concertRepository,
            ConcertPlaceRepository placeRepository,
            TicketOfficeRepository ticketOfficeRepository,
            ConcertImageRepository imageRepository,
            ConcertRedisRepository concertRedisRepository,
            ConcertKopisApiLogService kopisApiLogService,
            ArtistRepository artistRepository,
            ConcertArtistRepository concertArtistRepository
            ) {
        this.concertRepository = concertRepository;
        this.placeRepository = placeRepository;
        this.ticketOfficeRepository = ticketOfficeRepository;
        this.imageRepository = imageRepository;
        this.concertRedisRepository = concertRedisRepository;
        this.concertKopisApiLogService = kopisApiLogService;
        this.artistRepository = artistRepository;
        this.concertArtistRepository = concertArtistRepository;
        this.restClient = RestClient.builder()
                .baseUrl("https://kopis.or.kr/openApi/restful")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                .build();
    }

    @Retryable(value = HttpClientErrorException.class, backoff = @Backoff(delay = 5000))
    @Async
    @Transactional
    public void setConcertsList() throws InterruptedException {
        // 최초 시작 시간 저장
        if(concertKopisApiLogService.isInitComplete()) {
            log.error("이미 최초 저장이 되었습니다!. UpdateConcert를 통해 데이터를 갱신해주십시오!");
            return;
        }

        String key = "kopis_lock";
        String value = concertRedisRepository.lockGet(key);
        if(value != null) {
            log.error("이미 실행중인 스레드입니다.");
            return;
        } else {
            concertRedisRepository.lockSave(key,"running...");
        }

        // 시작 시간
        concertKopisApiLogService.saveStartLog("save","공연 데이터 초기 저장 시작",0L);
        LocalDateTime now = LocalDateTime.now();
        long startNs = System.currentTimeMillis();

        // 총 콘서트 요소를 저장할 배열
        List<ConcertListElement> totalConcertsList;
        // 저장시 캐시로 사용할 맵(어차피 400개 정도니까 맵 쓰는게 더 효율적으로 판단) -> 필드로 빼야 하나?
        Map<String, ConcertPlace> concertPlaceMap = new HashMap<>();

        int addedConcerts = 0;
        int addedConcertPlaces = 0;
        int addedTicketOffices = 0;
        int addedConcertImages = 0;
        int setArtists = 0;

        int page = 1;

        // 이전 실패가 있으면 실패 시점에서 다시 시도 아니면 0 에서 시작
        Long index = concertKopisApiLogService.getLastSaveFailIndex();
        try {
            // 모든 공연을 가져오기
            totalConcertsList = getAllConcertsListFromKopisAPI(page);
            log.info("저장할 총 공연의 수: {}", totalConcertsList.size());
            log.info("공연 목록 로드 완료, 공연 세부 내용 로드 및 저장");
            // 한국어 실명 기만 artistMap 가져오기
            Map<String, Artist> artistMap = getKoNameArtistMap();

            for(int i = index.intValue(); i < totalConcertsList.size(); i++) {
                ConcertListElement concertListElement = totalConcertsList.get(i);
                // API에서 공연 상세 가져오기
                ConcertDetailResponse concertDetailResponse = getConcertDetailResponse(serviceKey, concertListElement.getApiConcertId());
                Thread.sleep(120);
                ConcertDetailElement concertDetail = concertDetailResponse.getConcertDetail();

                // 콘서트 위치 저장
                // 콘서트 상세에서 저장할 콘서트 위치의 API ID 값 가져오기
                String concertPlaceAPiKey = concertDetailResponse.getConcertDetail().getMt10id();
                // 캐시로 사용하는 맵이나 DB에서 콘서트 위치가 있는지 확인하기 -> 없으면 저장
                ConcertPlace concertPlace = getConcertPlaceOrSaveNewConcertPlace(concertPlaceMap, concertPlaceAPiKey);

                addedConcertPlaces = concertPlaceMap.size();
                // 공연 저장
                Concert savedConcert = saveConcert(concertPlace, concertDetail);
                // 공연 예매처 저장
                addedTicketOffices += saveConcertTicketOffice(concertDetail, savedConcert);
                // 공연 이미지 저장
                addedConcertImages += saveConcertImages(concertDetail, savedConcert);
                // 공연 아티스트 연결
                setArtists += setConcertArtist(artistMap,concertDetail,savedConcert);
                addedConcerts++;
                index++;
            }

            concertKopisApiLogService.saveSuccessLog("save","공연 데이터 초기 저장 완료",0L);
            log.info(now + "시 기준 " + totalConcertsList.size() + "개의 공연 데이터 저장 완료!");
            long endNs = System.currentTimeMillis();
            long durationSec = ((endNs - startNs) / 1000);
            log.info(durationSec/60 + "분, " + durationSec % 60 + "초 소요되었습니다." );
            cacheClear();
        } catch (Exception e) {
            log.error("개별 공연 세부 내용 저장 도중 오류 발생");
            log.error("오류 내용 : " + e.getMessage());
            concertKopisApiLogService.saveErrorLog("save",e,index);
        } finally {
            concertRedisRepository.unlockSave(key);
        }
    }

    @Transactional
    public SetResultResponse updateConcertData() throws InterruptedException {
        String key = "kopis_lock";
        String value = concertRedisRepository.lockGet(key);
        if(value != null) {
            log.error("락이 걸린 작업입니다.");
            return null;
        } else{
            concertRedisRepository.lockSave(key,"running...");
        }


        LocalDate lastUpdatedDate = concertKopisApiLogService.getLastSaveTime().toLocalDate();
        concertKopisApiLogService.saveStartLog("save","공연 데이터 업데이트 시작",0L);
        LocalDate sdate = lastUpdatedDate;
        LocalDate edate = LocalDate.now().plusYears(1);

        SetResultResponse setResultResponse = new SetResultResponse();

        ConcertListResponse plr;
        List<ConcertListElement> totalConcertsList = new ArrayList<>();
        Map<String, ConcertPlace> concertPlaceMap = new HashMap<>();

        int page = 1;
        while (true) {
            plr = getConcertListResponse(serviceKey, sdate, edate, page, lastUpdatedDate);
            page++;
            if (plr.getConcertList() == null) break;
            for (ConcertListElement p : plr.getConcertList()) {
                totalConcertsList.add(p);
            }
            Thread.sleep(200);
        }
        log.info("공연 목록 로드 완료, 공연 세부 내용 로드 및 저장");

        // artist map 가져오기
        Map<String ,Artist> artistMap = getKoNameArtistMap();
        for (ConcertListElement performanceListElement : totalConcertsList) {
            ConcertDetailResponse concertDetailResponse = getConcertDetailResponse(serviceKey, performanceListElement.getApiConcertId());
            ConcertDetailElement concertDetail = concertDetailResponse.getConcertDetail();
            log.info("concert detail: " + concertDetailResponse.getConcertDetail());

            // 콘서트 위치 탐색 또는 추가
            String concertPlaceAPiKey = concertDetailResponse.getConcertDetail().getMt10id();
            ConcertPlace concertPlace = getConcertPlaceOrSaveNewConcertPlace(concertPlaceMap, concertPlaceAPiKey);

            // 콘서트 저장
            Concert concert = concertRepository.getConcertByApiConcertId(concertDetail.getApiConcertId());

            if (concert == null) {
                // 새 공연 저장
                Concert savedConcert = saveConcert(concertPlace,concertDetail);
                // 공연 예매처 저장
                setResultResponse.addedTicketOfficeAccumulator(saveConcertTicketOffice(concertDetail, savedConcert));
                // 공연 이미지 저장
                setResultResponse.addedConcertImagesAccumulator(saveConcertImages(concertDetail, savedConcert));
                // 공연 아티스트 저장
                setConcertArtist(artistMap,concertDetail,savedConcert);
                setResultResponse.addConcerts();
            } else {
                // 공연 데이터 갱신 후 저장
                Concert savedConcert = updateConcert(concert, concertPlace, concertDetail);
                // 기존에 저장되어 있던 연관 테이블 데이터 삭제
                ticketOfficeRepository.deleteByConcertId(savedConcert.getConcertId());
                imageRepository.deleteByConcertId(savedConcert.getConcertId());
                // 갱신된 데이터로 연관 테이블 저장
                setResultResponse.updatedTicketOfficesAccumulator(saveConcertTicketOffice(concertDetail, savedConcert));
                setResultResponse.updatedConcertImagesAccumulator(saveConcertImages(concertDetail, savedConcert));
                setResultResponse.addUpdatedConcerts();
            }

            Thread.sleep(300);
        }

        // 갱신 후 업데이트 시간 저장
        concertKopisApiLogService.saveSuccessLog("save","공연 데이터 업데이트 완료",0L);
        // 락 해제
        concertRedisRepository.unlockSave(key);
        // 이전 캐시 데이터 삭제
        cacheClear();
        return setResultResponse;
    }



    @Transactional
    public void concertUpdateByKopisApi(Long concertId){
        // 해당 콘서트 ID로 콘서트 객체 찾기
        Concert concert = concertRepository.getConcertByConcertId(concertId);

        // 콘서트 객체에서 API ID 값 찾아서 콘서트 상세 조회
        ConcertDetailResponse concertDetailResponse = getConcertDetailResponse(serviceKey,concert.getApiConcertId());
        ConcertDetailElement concertDetail = concertDetailResponse.getConcertDetail();

        // 콘서트 상세에서 콘서트 장소 API ID 값 통해서 콘서트 장소 찾기
        String concertPlaceAPiKey = concertDetailResponse.getConcertDetail().getMt10id();
        ConcertPlace concertPlace = placeRepository.getConcertPlaceByApiConcertPlaceId(concertPlaceAPiKey);
        if (concertPlace == null) {
            // 콘서트 장소가 null일시 -> DB에 없는 새로운 콘서트 장소 -> DB 저장
            ConcertPlaceDetailResponse concertPlaceDetailElement = getConcertPlaceDetailResponse(serviceKey, concertPlaceAPiKey);
            ConcertPlaceDetailElement concertPlaceDetail = concertPlaceDetailElement.getConcertPlaceDetail();
            concertPlace = concertPlaceDetail.getConcertPlace();
            ConcertPlace savedConcertPlace = placeRepository.save(concertPlace);
            log.info("concert place saved: " + savedConcertPlace);
        }
        // 공연의 정보를 새로운 정보로 변경
        Concert savedConcert = updateConcert(concert, concertPlace, concertDetail);
        // 기존에 저장되어 있던 연관 테이블 데이터 삭제
        ticketOfficeRepository.deleteByConcertId(savedConcert.getConcertId());
        imageRepository.deleteByConcertId(savedConcert.getConcertId());
        // 새로 받아온 예매처, 이미지 데이터 저장
        saveConcertTicketOffice(concertDetail, savedConcert);
        saveConcertImages(concertDetail, savedConcert);


    }


    private List<ConcertListElement> getAllConcertsListFromKopisAPI(int page) throws InterruptedException {
        List<ConcertListElement> totalConcertsList = new ArrayList<>();
        while (true) {
            // 콘서트 목록 받아오기
            ConcertListResponse plr = getConcertListResponse(serviceKey, sdate, edate, page);
            page++;
            // 더 이상 받아올 콘서트 목록이 없으면 멈춤
            if (plr.getConcertList() == null) break;
            // 콘서트 요소를 콘서트 목록에서 꺼내서 더하기
            totalConcertsList.addAll(plr.getConcertList());
            log.info("Total Concert List: {}", totalConcertsList.size() + "개의 데이터 가져오는중...");
            Thread.sleep(120);
        }
        return totalConcertsList;
    }

    // 공연 데이터 저장
    private Concert saveConcert(ConcertPlace concertPlace, ConcertDetailElement concertDetail) {
        TicketPrice ticketPrice = new TicketPrice(concertDetail.getConcertPrice());
        Concert concert = new Concert(
                concertPlace,
                concertDetail.getConcertName(),
                concertDetail.getConcertDescription(),
                dateStringToDateTime(concertDetail.getStartDate()),
                dateStringToDateTime(concertDetail.getEndDate()),
                null,
                null,
                ticketPrice.maxPrice,
                ticketPrice.minPrice,
                concertDetail.getPosterUrl(),
                concertDetail.getArea(),
                concertDetail.getApiConcertId()
        );
        return concertRepository.save(concert);
    }

    // 공연 정보를 새로운 정보로 갱신해서 DB에 저장
    private Concert updateConcert(Concert concert, ConcertPlace concertPlace, ConcertDetailElement concertDetail) {
        TicketPrice ticketPrice = new TicketPrice(concertDetail.getConcertPrice());
        concert = concert.updateByAPI(
                concertPlace,
                concertDetail.getConcertDescription(),
                dateStringToDateTime(concertDetail.getStartDate()),
                dateStringToDateTime(concertDetail.getEndDate()),
                ticketPrice.maxPrice,
                ticketPrice.minPrice,
                concertDetail.getPosterUrl()
        );

        return concertRepository.save(concert);
    }

    // 공연 장소를 주어진 map에서 찾고 없으면 DB에서 찾음, DB에서도 없으면 API에서 해당 데이터를 찾아서 저장 후 반환
    private ConcertPlace getConcertPlaceOrSaveNewConcertPlace(Map<String, ConcertPlace> concertPlaceMap, String concertPlaceAPiKey) throws InterruptedException {
        ConcertPlace concertPlace = concertPlaceMap.getOrDefault(concertPlaceAPiKey, placeRepository.getConcertPlaceByApiConcertPlaceId(concertPlaceAPiKey));

        if (concertPlace == null) {
            // 맵이나 DB에 없다면 API에서 해당 콘서트 위치를 가져와서 DB에 저장 후 캐시에 저장
            ConcertPlaceDetailResponse concertPlaceDetailElement = getConcertPlaceDetailResponse(serviceKey, concertPlaceAPiKey);
            Thread.sleep(120);
            ConcertPlaceDetailElement concertPlaceDetail = concertPlaceDetailElement.getConcertPlaceDetail();
            concertPlace = concertPlaceDetail.getConcertPlace();
            ConcertPlace savedConcertPlace = placeRepository.save(concertPlace);
            concertPlaceMap.put(concertPlaceAPiKey, savedConcertPlace);
        }

        return concertPlace;
    }

    // 모든 Artist를 가져와서 실명 - Artist객체의 맵으로 변환 후 반환
    private Map<String, Artist> getKoNameArtistMap() {
        List<Artist> artistList = artistRepository.findAll();
        Map<String, Artist> artistMap = new HashMap<>();
        for (Artist artist : artistList) {
            artistMap.put(artist.getNameKo(), artist);
        }
        return artistMap;
    }

    // 공연에 Artist매칭해서 저장 후 저장된 개수 반환
    private int setConcertArtist(Map<String,Artist> artistMap,ConcertDetailElement concertDetail, Concert savedConcert) {
        String rawCast = concertDetail.getConcertCast();
        rawCast = rawCast.replace("," ,"");
        String[] casts = rawCast.split(" ");
        List<ConcertArtist> concertArtistList = new ArrayList<>();
        for(String cast : casts) {
            Artist artist = artistMap.get(cast);
            if(artist == null) continue;
            ConcertArtist concertArtist = new ConcertArtist(artist,savedConcert);
            concertArtistList.add(concertArtist);
        }
        if(concertArtistList.isEmpty()) return 0;
        else {
            concertArtistRepository.saveAll(concertArtistList);
            return concertArtistList.size();
        }
    }

    // 콘서트 예매처를 저장합니다.
    private int saveConcertTicketOffice(ConcertDetailElement concertDetail, Concert savedConcert) {
        List<TicketOfficeResponse> ticketOfficeResponses = concertDetail.getTicketOfficeResponses();

        for (TicketOfficeResponse ticketOffice : ticketOfficeResponses) {
            TicketOffice to = new TicketOffice(
                    savedConcert,
                    ticketOffice.getTicketOfficeName(),
                    ticketOffice.getTicketOfficeUrl()
            );
            ticketOfficeRepository.save(to);
        }
        return ticketOfficeResponses.size();
    }

    // 콘서트 이미지를 저장합니다.
    private int saveConcertImages(ConcertDetailElement concertDetail, Concert savedConcert) {
        List<ConcertImage> concertImages = new ArrayList<>();
        for (String imageUrl : concertDetail.getConcertImageUrls()) {
            ConcertImage concertImage = new ConcertImage(savedConcert, imageUrl);
            concertImages.add(concertImage);
        }
        imageRepository.saveAll(concertImages);

        return concertDetail.getConcertImageUrls().size();
    }

    // 캐시를 초기화합니다.
    private void cacheClear() {
        concertRedisRepository.deleteAllConcertsList();
        concertRedisRepository.deleteAllCachedConcertDetail();
        concertRedisRepository.deleteTotalConcertsCount(ListSort.VIEW);
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

    //콘서트 목록을 조회합니다.
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


    //특정 날짜 이후로 갱신된 콘서트 목록을 조회합니다.
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

    //콘서트 상세를 조회합니다.
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



    // 콘서트 장소 목록을 조회합니다.
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

    // 콘서트 장소 목록을 이름 기준으로 조회합니다.
    private ConcertPlaceListResponse getConcertPlaceListResponseByName(String serviceKey, String name, int page) {
        return restClient.get()
                .uri(uriBuilder ->
                        uriBuilder.path("/prfplc")
                                .queryParam("service", serviceKey)
                                .queryParam("cpage", page)
                                .queryParam("rows", 100)
                                .queryParam("shprfnmfct", name)
                                .build()
                ).retrieve().body(ConcertPlaceListResponse.class);
    }

    //콘서트 장소 상세를 조회합니다.
    private ConcertPlaceDetailResponse getConcertPlaceDetailResponse(String serviceKey, String concertPlaceId) {
        return restClient.get()
                .uri(uriBuilder ->
                        uriBuilder.path("/prfplc/" + concertPlaceId)
                                .queryParam("service", serviceKey)
                                .build()
                ).retrieve().body(ConcertPlaceDetailResponse.class);
    }

    // API에서 출력하는 날짜 문자열을 LocalDate 객체로 변환
    private LocalDate dateStringToDateTime(String dateString) {
        // "yyyy.MM.dd" 형식으로 들어옴
        String[] split = dateString.split("\\.");
        int year = Integer.parseInt(split[0]);
        int month = Integer.parseInt(split[1]);
        int day = Integer.parseInt(split[2]);
        return LocalDate.of(year, month, day);
    }

    // 가격을 저장할 내부 객체
    private class TicketPrice {
        int maxPrice;
        int minPrice;

        // 문자열 가격 정보를 받아서 변환 후 저장.
        public TicketPrice(String price) {
            String[] bits = price.split(" ");
            maxPrice = 0;
            minPrice = Integer.MAX_VALUE;
            if (bits.length == 1) {
                minPrice = 0;
            } else {
                for (String bit : bits) {
                    bit = bit.replaceAll(",", "");
                    if (bit.endsWith("원")) {
                        bit = bit.replaceAll("원", "");
                        this.maxPrice = Math.max(Integer.parseInt(bit), maxPrice);
                        this.minPrice = Math.min(Integer.parseInt(bit), minPrice);
                    }
                }
            }
        }
    }
}
