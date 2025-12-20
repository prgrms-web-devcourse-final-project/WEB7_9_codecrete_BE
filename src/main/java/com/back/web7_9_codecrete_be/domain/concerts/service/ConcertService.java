package com.back.web7_9_codecrete_be.domain.concerts.service;

import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.*;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concertPlace.PlaceDetailResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.ticketOffice.TicketOfficeElement;
import com.back.web7_9_codecrete_be.domain.concerts.entity.*;
import com.back.web7_9_codecrete_be.domain.concerts.repository.*;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.global.error.code.ConcertErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConcertService {
    private final ConcertRepository concertRepository;

    private final ConcertLikeRepository concertLikeRepository;

    private final ConcertPlaceRepository concertPlaceRepository;

    private final TicketOfficeRepository ticketOfficeRepository;

    private final ConcertImageRepository concertImageRepository;

    private final ConcertRedisRepository concertRedisRepository;

    // 공연 목록 조회
    public List<ConcertItem> getConcertsList(Pageable pageable, ListSort sort) {
        switch (sort) {
            case LIKE -> {
                return concertRepository.getConcertItemsOrderByLikeCountDesc(pageable);
            }
            case VIEW -> {
                return concertRepository.getConcertItemsOrderByViewCountDesc(pageable);
            }
            case TICKETING -> {
                return concertRepository.getUpComingTicketingConcertItemsFromDateASC(pageable, LocalDateTime.of(LocalDate.now(), LocalTime.MIN));
            }
            case UPCOMING -> {
                return concertRepository.getUpComingConcertItemsFromDateASC(pageable,LocalDate.now());
            }
            case REGISTERED -> {
                return concertRepository.getConcertItemsOrderByApiId(pageable);
            }
        }

        return concertRepository.getConcertItems(pageable);
    }

    // 사용자가 좋아요 한 공연 목록 조회
    public List<ConcertItem> getLikedConcertsList(Pageable pageable,User user) {
        return concertRepository.getLikedConcertsList(pageable, user.getId());
    }

    // 티켓팅 시간이 없는 공연 목록 조회
    public List<ConcertItem> getNoTicketTimeConcertsList(Pageable pageable) {
        return concertRepository.getNoTicketTimeConcertList(pageable);
    }

    // 키워드 통한 공연 제목 검색
    public List<ConcertItem> getConcertListByKeyword(String keyword, Pageable pageable) {
        if(keyword == null || keyword.isEmpty()){
            throw new BusinessException(ConcertErrorCode.KEYWORD_IS_NULL);
        }
        return concertRepository.getConcertItemsByKeyword(keyword, pageable);
    }

    // 공연 상세 조회 조회시 조회수 1 증가 -> 캐싱에 따른 조회수 불일치 해소를 어떻게 할 것인가?
    @Transactional
    public ConcertDetailResponse getConcertDetail(long concertId) {
        ConcertDetailResponse concertDetailResponse = concertRedisRepository.getDetail(concertId);
        if(concertDetailResponse == null){
            concertDetailResponse = concertRepository.getConcertDetailById(concertId);
            List<ConcertImage>  concertImages = concertImageRepository.getConcertImagesByConcert_ConcertId(concertId);
            List<String> concertImageUrls = new ArrayList<>();
            for(ConcertImage concertImage : concertImages){
                concertImageUrls.add(concertImage.getImageUrl());
            }

            concertRepository.concertViewCountUp(concertId);
            concertDetailResponse.setConcertImageUrls(concertImageUrls);
            concertDetailResponse.setViewCount(concertDetailResponse.getViewCount() + 1);
            concertRedisRepository.detailSave(concertId, concertDetailResponse);
        }
        return concertDetailResponse;
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

    // 공연 시간 설정
    public ConcertDetailResponse setConcertTicketingTime(ConcertTicketTimeSetRequest concertTicketTimeSetRequest) {
        Concert concert = findConcertByConcertId(concertTicketTimeSetRequest.getConcertId());
        concert.ticketTimeSet(concertTicketTimeSetRequest.getTicketTime());
        Concert savedConcert = concertRepository.save(concert);
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

    private Concert findConcertByConcertId(long concertId) {
        return concertRepository.findById(concertId).orElseThrow(
                () -> new BusinessException(ConcertErrorCode.CONCERT_NOT_FOUND)
        );
    }

}
