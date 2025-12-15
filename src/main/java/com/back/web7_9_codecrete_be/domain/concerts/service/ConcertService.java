package com.back.web7_9_codecrete_be.domain.concerts.service;

import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.*;
import com.back.web7_9_codecrete_be.domain.concerts.dto.ticketOffice.TicketOfficeElement;
import com.back.web7_9_codecrete_be.domain.concerts.entity.*;
import com.back.web7_9_codecrete_be.domain.concerts.repository.*;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.domain.users.repository.UserRepository;
import com.back.web7_9_codecrete_be.global.error.code.AuthErrorCode;
import com.back.web7_9_codecrete_be.global.error.code.ConcertErrorCode;
import com.back.web7_9_codecrete_be.global.error.code.ErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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


    public List<ConcertItem> getConcertsList(Pageable pageable) {
        return concertRepository.getConcertItems(pageable);
    }

    public List<ConcertItem> getUpcomingConcertsList(Pageable pageable) {
        return concertRepository.getUpComingConcertItems(pageable, LocalDate.now());
    }

    public List<ConcertItem> getLikedConcertsList(Pageable pageable,User user) {
        return concertRepository.getLikedConcertsList(pageable, user.getId());
    }

    public List<ConcertItem> getNoTicketTimeConcertsList(Pageable pageable) {
        return concertRepository.getNoTicketTimeConcertList(pageable);
    }

    public ConcertDetailResponse getConcertDetail(long concertId) {
        ConcertDetailResponse concertDetailResponse = concertRepository.getConcertDetailById(concertId);
        List<ConcertImage>  concertImages = concertImageRepository.getConcertImagesByConcert_ConcertId(concertId);
        List<String> concertImageUrls = new ArrayList<>();
        for(ConcertImage concertImage : concertImages){
            concertImageUrls.add(concertImage.getImageUrl());
        }
        concertDetailResponse.setConcertImageUrls(concertImageUrls);
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

    public List<TicketOfficeElement> getTicketOfficesList(long concertId) {
        Concert concert = new Concert(concertId);
        List<TicketOffice> ticketOffices = ticketOfficeRepository.getTicketOfficesByConcert(concert);
        List<TicketOfficeElement> ticketOfficeList = new ArrayList<>();
        for (TicketOffice ticketOffice : ticketOffices) {
            ticketOfficeList.add(new TicketOfficeElement(ticketOffice));
        }

        return ticketOfficeList;
    }

    public ConcertLikeResponse isLikeConcert(Long concertId, User user) {
        Concert concert = concertRepository.getConcertByConcertId(concertId);
        ConcertLikeResponse concertLikeResponse;
        if(concertLikeRepository.existsConcertLikeByConcertAndUser(concert,user)){
            concertLikeResponse = new ConcertLikeResponse(concert,true);
        } else {
            concertLikeResponse = new ConcertLikeResponse(concert,false);
        }

        return concertLikeResponse;
    }

    public void likeConcert(long concertId, User user) {
        Concert concert = concertRepository.findById(concertId).orElseThrow();
        if(concertLikeRepository.existsConcertLikeByConcertAndUser(concert,user)){
            throw new BusinessException(ConcertErrorCode.LIKE_CONFLICT);
        }
        ConcertLike concertLike = new ConcertLike(concert, user);
        concertLikeRepository.save(concertLike);
    }

    public void dislikeConcert(long concertId, User user) {
        Concert concert = concertRepository.findById(concertId).orElseThrow();
        ConcertLike concertLike = concertLikeRepository.findConcertLikeByConcertAndUser(concert, user);
        if(concertLike == null){
            throw new BusinessException(ConcertErrorCode.NOT_FOUND_CONCERTLIKE);
        }
        concertLikeRepository.delete(concertLike);
    }

    public ConcertItem updateConcert(long concertId, ConcertUpdateRequest concertUpdateRequest) {
        Concert concert = concertRepository.findById(concertId).orElseThrow();
        ConcertPlace concertPlace = concertPlaceRepository.findById(concertUpdateRequest.getPlaceId()).orElseThrow();
        concert.update(concertUpdateRequest, concertPlace);
        Concert updatedConcert = concertRepository.save(concert);
        return new ConcertItem(updatedConcert);
    }

    public ConcertDetailResponse setConcertTime(ConcertTicketTimeSetRequest concertTicketTimeSetRequest) {
        Concert concert = concertRepository.findById(concertTicketTimeSetRequest.getConcertId()).orElseThrow();
        concert.ticketTimeSet(concertTicketTimeSetRequest.getTicketTime());
        Concert savedConcert = concertRepository.save(concert);
        return concertRepository.getConcertDetailById(savedConcert.getConcertId());
    }

    public void deleteConcert(long concertId) {
        Concert concert = concertRepository.findById(concertId).orElseThrow();
        concertRepository.deleteById(concertId);
    }


}
