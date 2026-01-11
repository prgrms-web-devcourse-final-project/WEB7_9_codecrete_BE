package com.back.web7_9_codecrete_be.domain.concerts.service;

import com.back.web7_9_codecrete_be.domain.artists.entity.Artist;
import com.back.web7_9_codecrete_be.domain.artists.entity.ConcertArtist;
import com.back.web7_9_codecrete_be.domain.artists.repository.ConcertArtistRepository;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.*;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.WeightedString;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concertPlace.PlaceDetailResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.ticketOffice.TicketOfficeElement;
import com.back.web7_9_codecrete_be.domain.concerts.entity.*;
import com.back.web7_9_codecrete_be.domain.concerts.repository.*;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.global.error.code.ConcertErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConcertService {
    private final ConcertRepository concertRepository;

    private final ConcertLikeRepository concertLikeRepository;

    private final ConcertPlaceRepository concertPlaceRepository;

    private final TicketOfficeRepository ticketOfficeRepository;

    private final ConcertImageRepository concertImageRepository;

    private final ConcertRedisRepository concertRedisRepository;

    private final ConcertSearchRedisTemplate concertSearchRedisTemplate;

    private final ConcertArtistRepository concertArtistRepository;

    // 공연 목록 조회
    public List<ConcertItem> getConcertsList(Pageable pageable, ListSort sort) {
        List<ConcertItem> concertItems;
        concertItems = concertRedisRepository.getConcertsList(pageable, sort);

        if(concertItems != null && !concertItems.isEmpty()) return concertItems;

        switch (sort) {
            case LIKE -> concertItems = concertRepository.getConcertItemsOrderByLikeCountDesc(pageable);
            case VIEW -> concertItems = concertRepository.getConcertItemsOrderByViewCountDesc(pageable);
            case TICKETING -> concertItems = concertRepository.getUpComingTicketingConcertItemsFromDateASC(pageable, LocalDateTime.of(LocalDate.now(), LocalTime.MIN));
            case UPCOMING -> concertItems = concertRepository.getUpComingConcertItemsFromDateASC(pageable,LocalDate.now());
            case REGISTERED -> concertItems = concertRepository.getConcertItemsOrderByApiIdDesc(pageable);
        }

        concertRedisRepository.saveConcertsList(sort,pageable,concertItems);
        return concertItems;
    }

    // 사용자가 좋아요 한 공연 목록 조회
    public List<ConcertItem> getLikedConcertsList(Pageable pageable,User user) {
        return concertRepository.getLikedConcertsList(pageable, user.getId());
    }

    // 티켓팅 시간이 없는 공연 목록 조회
    public List<ConcertItem> getNoTicketTimeConcertsList(Pageable pageable) {
        return concertRepository.getNoTicketTimeConcertList(pageable);
    }

    // 아티스트 기준 공연 목록 조회
    public List<ConcertItem> getArtistConcertList(Long artistId, String type, Pageable pageable) {
        return switch (type) {
            case "past" -> concertRepository.getPastConcertsListByArtist(artistId, pageable);
            case "upcoming" -> concertRepository.getUpcomingConcertsListByArtist(artistId, pageable);
            case "all" -> concertRepository.getAllConcertsListByArtist(artistId);
            default -> throw new BusinessException(ConcertErrorCode.INCORRECT_TYPE);
        };
    }

    // 키워드 통한 공연 제목 검색
    public List<ConcertItem> getConcertListByKeyword(String keyword, Pageable pageable) {
        if(keyword == null || keyword.isEmpty()){
            throw new BusinessException(ConcertErrorCode.KEYWORD_IS_NULL);
        }
        return concertRepository.getConcertItemsByKeyword(keyword, pageable);
    }

    // 키워드 통한 공연 제목 검색 결과 개수
    public Integer getConcertSearchCountByKeyword(String keyword) {
        if(keyword == null || keyword.isEmpty()){
            throw new BusinessException(ConcertErrorCode.KEYWORD_IS_NULL);
        }
        return concertRepository.countConcertsByNameContaining(keyword);
    }

    // 검색어 자동 완성
    public List<AutoCompleteItem> autoCompleteSearch(String keyword, int start, int end) {
        return concertSearchRedisTemplate.getAutoCompleteWord(keyword, start, end);
    }

    // 자동완성 초기화
    public void resetAutoComplete(){
        concertSearchRedisTemplate.deleteAutoCompleteWords();
    }

    // 자동완성 단어저장 v2
    public void setAutoComplete(){
        List<Concert>  concerts = concertRepository.findAll();
        List<WeightedString> weightedStrings = concerts.stream()
                .map(WeightedString::new)
                .toList();
        concertSearchRedisTemplate.addAllWordsWithWeight(weightedStrings);
    }


    // 공연 상세 조회 조회시 조회수 1 증가 -> 캐싱에 따른 조회수 불일치 해소를 어떻게 할 것인가? V -> 이제 캐싱된거 날리고 새로운 수치 반영 어케할 것인지 + 여러번 조회수 올릴 시 처리 어떻게 할지
    @Transactional
    public ConcertDetailResponse getConcertDetail(long concertId) {
        ConcertDetailResponse concertDetailResponse = concertRedisRepository.getCachedConcertDetail(concertId);
        if(concertDetailResponse == null){
            concertDetailResponse = concertRepository.getConcertDetailById(concertId);
            List<String> concertImageUrls = concertImageRepository.getConcertImagesByConcert_ConcertId(concertId).stream()
                    .map(ConcertImage::getImageUrl)
                    .toList();
            concertDetailResponse.setConcertImageUrls(concertImageUrls);
            List<Long> concertArtists = concertArtistRepository.getConcertArtistsByConcert_ConcertId(concertId).stream()
                    .map(ConcertArtist::getArtist)
                    .map(Artist::getId)
                    .toList();
            concertDetailResponse.setConcertArtists(concertArtists);
        }
        // 조회수 1 증가하고 해당 데이터를 캐시에 저장.
        concertDetailResponse.setViewCount(concertDetailResponse.getViewCount() + 1);
        concertRedisRepository.saveConcertDetail(concertId, concertDetailResponse);
        return concertDetailResponse;
    }

    // 조회수 갱신
    @Transactional
    public void viewCountUpdate(){
        Map<Long,Integer> viewCountMap = concertRedisRepository.getCachedViewCountMap();
        if(viewCountMap == null || viewCountMap.isEmpty()) {
            log.info("viewCountMap is empty");
        } else{
            for (Map.Entry<Long, Integer> viewCountEntry : viewCountMap.entrySet()) {
                concertRepository.concertViewCountSet(viewCountEntry.getKey(), viewCountEntry.getValue());
            }
            concertRedisRepository.deleteAllConcertsList();
            log.info("viewCount updated");
        }
    }

    // 총 공연 개수 조회
    public Long getTotalConcertsCount() {
        Long result = concertRedisRepository.getTotalConcertsCount(ListSort.VIEW);
        if(result == -1) result = concertRedisRepository.saveTotalConcertsCount(concertRepository.count(), ListSort.VIEW);
        return result;
    }

    // 티켓팅 공연 개수 조회
    public Long getTotalTicketingConcertsCount() {
        Long result = concertRedisRepository.getTotalConcertsCount(ListSort.TICKETING);
        if(result == -1) result = concertRedisRepository.saveTotalConcertsCount(concertRepository.countTicketingConcertsFromLocalDateTime(LocalDateTime.of(LocalDate.now(), LocalTime.MIN)), ListSort.TICKETING);
        return  result;
    }

    // 좋아요한 공연 개수 조회
    public Long getTotalLikedConcertsCount(User user) {
        Long result = concertRedisRepository.getUserLikedCount(user);
        if(result == -1) result = concertRedisRepository.saveUserLikedCount(user,concertLikeRepository.countByUser(user));
        return  result;
    }

    // N+1 문제 발생해서 버림
    /*
    public List<ConcertItem> getConcertsList2(Pageable pageable) {
        List<Concert> concerts = concertRepository.findAll(pageable).getContent();
        List<ConcertItem> concertItems = new ArrayList<>();
        for (Concert concert : concerts) {
            concertItems.add(new ConcertItem(concert));
        }
        return concertItems;
    }
    */

    // 공연 예매처 조회
    public List<TicketOfficeElement> getTicketOfficesList(long concertId) {
        List<TicketOffice> ticketOffices = ticketOfficeRepository.getTicketOfficesByConcert_ConcertId(concertId);
        List<TicketOfficeElement> ticketOfficeList = new ArrayList<>();
        for (TicketOffice ticketOffice : ticketOffices) {
            ticketOfficeList.add(new TicketOfficeElement(ticketOffice));
        }

        return ticketOfficeList;
    }

    // 공연 좋아요 여부 확인
    public ConcertLikeResponse isLikeConcert(Long concertId, User user) {
        Concert concert = findConcertByConcertId(concertId);
        ConcertLikeResponse concertLikeResponse;
        if(concertLikeRepository.existsConcertLikeByConcertAndUser(concert,user)){
            concertLikeResponse = new ConcertLikeResponse(concert,true);
        } else {
            concertLikeResponse = new ConcertLikeResponse(concert,false);
        }

        return concertLikeResponse;
    }

    // 사용자가 해당 공연에 좋아요
    @Transactional
    public void likeConcert(long concertId, User user) {
        Concert concert = findConcertByConcertId(concertId);

        if(concertLikeRepository.existsConcertLikeByConcertAndUser(concert,user)){
            throw new BusinessException(ConcertErrorCode.LIKE_CONFLICT);
        }
        ConcertLike concertLike = new ConcertLike(concert, user);
        concertLikeRepository.save(concertLike);
        concertRedisRepository.deleteUserLikedCount(user);
        concertRepository.concertLikeCountUp(concertId);
    }

    // 사용자가 해당 공연에 좋아요 해제
    @Transactional
    public void dislikeConcert(long concertId, User user) {
        Concert concert = findConcertByConcertId(concertId);

        ConcertLike concertLike = concertLikeRepository.findConcertLikeByConcertAndUser(concert, user);
        if(concertLike == null){
            throw new BusinessException(ConcertErrorCode.NOT_FOUND_CONCERTLIKE);
        }
        concertLikeRepository.delete(concertLike);
        concertRedisRepository.deleteUserLikedCount(user);
        concertRepository.concertLikeCountDown(concertId);
    }

    // 공연 내용 갱신
    public ConcertItem updateConcert(long concertId, ConcertUpdateRequest concertUpdateRequest) {
        Concert concert = findConcertByConcertId(concertId);
        ConcertPlace concertPlace = concertPlaceRepository.findById(concertUpdateRequest.getPlaceId()).orElseThrow();
        concert.update(concertUpdateRequest, concertPlace);
        Concert updatedConcert = concertRepository.save(concert);
        return new ConcertItem(updatedConcert);
    }

    // 공연 예매 시간 설정
    public ConcertDetailResponse setConcertTicketingTime(ConcertTicketTimeSetRequest concertTicketTimeSetRequest) {
        Concert concert = findConcertByConcertId(concertTicketTimeSetRequest.getConcertId());
        LocalDateTime ticketTime = concertTicketTimeSetRequest.getTicketTime();
        LocalDateTime ticketEndTime = concertTicketTimeSetRequest.getTicketEndTime();

        if(ticketTime.isAfter(ticketEndTime)) throw new BusinessException(ConcertErrorCode.NOT_VALID_TICKETING_TIME);
        if(ticketTime.isAfter(concert.getEndDate().atTime(LocalTime.MAX))) throw new BusinessException(ConcertErrorCode.CONCERT_TICKETING_TIME_IS_NOT_AFTER_CONCERT_END_DATE);
        if(ticketEndTime.isAfter(concert.getEndDate().atTime(LocalTime.MAX))) throw new BusinessException(ConcertErrorCode.CONCERT_TICKETING_END_TIME_IS_NOT_AFTER_CONCERT_END_DATE);

        concert.ticketTimeSet(ticketTime, ticketEndTime);
        // DB에 저장
        Concert savedConcert = concertRepository.save(concert);
        // 캐시에도 갱신
        concertRedisRepository.updateCachedTickingDate(concert.getConcertId(),ticketTime,ticketEndTime);
        return concertRepository.getConcertDetailById(savedConcert.getConcertId());
    }

    // 공연 삭제
    public void deleteConcert(long concertId) {
        concertRepository.deleteById(concertId);
    }

    // 아티스트 Id 리스트로 해당 아티스트들의 공연 목록 조회
    public List<Concert> findConcertsByArtistIds(List<Long> artistIds) {
        return concertRepository.findDistinctByArtistIds(artistIds);
    }

    // 공연 시설 조회
    public PlaceDetailResponse getConcertPlaceDetail(long concertId) {
        ConcertPlace concertPlace = concertPlaceRepository.getConcertPlaceByConcertId(concertId);
        return new PlaceDetailResponse(concertPlace);
    }

    // 공연 정보 조회
    private Concert findConcertByConcertId(long concertId) {
        return concertRepository.findById(concertId).orElseThrow(
                () -> new BusinessException(ConcertErrorCode.CONCERT_NOT_FOUND)
        );
    }

    // 같은 위치에 시작하는 공연
    public List<ConcertItem> recommendSimilarConcerts(long concertId) {
        Concert concert = findConcertByConcertId(concertId);
        return concertRepository.getSimilarConcerts(
                concertId,
                concert.getConcertPlace().getConcertPlaceId(),
                concert.getStartDate(),
                concert.getStartDate().plusDays(60)
        );
    }

    // 유사한 제목을 가지는 공연 추천
    public List<ConcertItem> recommendSimilarTitleConcerts(long concertId) {
        Concert concert = findConcertByConcertId(concertId);
        String name = simplifyKeyword(concert.getName());
        log.info("name: " + name);
        String[] words = name.split(" ");
        List<AutoCompleteItem> result = new ArrayList<>();

        for (String word : words) {
            if(word.isEmpty()) continue;
            log.info("word: " + word);
            result.addAll(concertSearchRedisTemplate.getAutoCompleteWord(word,0,5));
        }
        List<Long> idList = new ArrayList<>();

        for (AutoCompleteItem item : result) {
            if(Objects.equals(concert.getConcertId(), item.getId())) continue;
            idList.add(item.getId());
        }

        // 자카드 유사도를 통해 더 비슷한 항목이 위로 오게 정렬하기
        List<ConcertItem> concertItemList = concertRepository.getConcertItemsInIdList(idList,LocalDate.now());
        concertItemList.sort(Comparator.comparingDouble(i1 -> jaccardSimilarity(words,i1.getName().split(" "))));
        return concertItemList;
    }

    // 좋아요 한 제목에서 중복으로 나타나는 단어에 가중치 부여 후 자카드 유사도에 가점 부여하여 정렬 후 공연 추천
    public List<ConcertItem> concertsRecommendByLike(User user){
        Pageable pageable = PageRequest.of(0, 100);
        List<ConcertItem> likeList = concertRepository.getLikedConcertsList(pageable,user.getId());
        if(likeList.isEmpty()) return new ArrayList<>(); // 좋아요 한 공연이 없을 경우 빈 공연 반환
        Map<String, WeightedBits> weightedBitsMap = new HashMap<>();
        Set<Long> idSet = new HashSet<>();
        for(ConcertItem item : likeList){
            idSet.add(item.getId());
            String name = item.getName();
            String simpleName = simplifyKeyword(name);
            String[] words = simpleName.split(" ");

            for (String word : words) { // 단어별로 가중치 적용
               WeightedBits weightedBits = weightedBitsMap.getOrDefault(word, new WeightedBits(word,0));
               weightedBits.plusWeight();
               weightedBitsMap.put(word, weightedBits);
            }
        }

        List<AutoCompleteItem> result = new ArrayList<>();
        for (String word : weightedBitsMap.keySet()) {
            if(word.isEmpty()) continue;
            log.info("word: " + word);
            result.addAll(concertSearchRedisTemplate.getAutoCompleteWord(word,0,6));
        }

        Set<Long> resultIdSet = new HashSet<>();
        for (AutoCompleteItem item : result) {
            if(idSet.contains(item.getId())) continue; // 찜한 목록 제거
            resultIdSet.add(item.getId()); // 중복 제거
        }

        List<Long> idList = new ArrayList<>();
        for (Long id : resultIdSet) {
            idList.add(id);
        }

        List<ConcertItem> concertItemList = concertRepository.getConcertItemsInIdList(idList,LocalDate.now());
        concertItemList.sort(Comparator.comparingDouble(
                ci -> jaccardSimilarityWithWeight(weightedBitsMap,ci.getName().split(" "))
        ));
        return concertItemList;
    }

    private double jaccardSimilarityWithWeight(Map<String,WeightedBits> weightedBitsMap, String[] words) {
        int intersection =0;
        for (String word : words) {
            if(word.isEmpty()) continue;
            WeightedBits weightedBits = weightedBitsMap.get(word);
            if(weightedBits == null) continue;
            intersection += weightedBits.weight;
        }
        int union = weightedBitsMap.size() + words.length;
        return (double)union/intersection;
    }

    private class WeightedBits{
        String bit;
        int weight;

        public WeightedBits(String bit, int weight) {
            this.bit = bit;
            this.weight = weight;
        }

        public WeightedBits plusWeight(){
            this.weight++;
            return this;
        }
    }

    private double jaccardSimilarity(String[] origin , String[] target) {
        Set<String> union = new HashSet<>();
        Set<String> intersection = new HashSet<>();
        union.addAll(Arrays.asList(origin));
        union.addAll(Arrays.asList(target));

        for (String s : origin) {
            for (String t : target) {
                if (s.equals(t)) intersection.add(s);
            }
        }

        return (double)  union.size() / intersection.size();
    }

    // 특수 문자를 제거합니다.
    private static String simplifyKeyword(String name) {
        String match = "[^ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z0-9\\s]";
        name = name.replaceAll(match, "");
        return name;
    }

    @Transactional(readOnly = true)
    public void validateConcertExists(Long concertId) {
        concertRepository.findById(concertId)
                .orElseThrow(() ->
                        new BusinessException(ConcertErrorCode.CONCERT_NOT_FOUND)
                );
    }

}
