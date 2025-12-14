package com.back.web7_9_codecrete_be.domain.concerts.service;

import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertDetailResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertItem;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertUpdateRequest;
import com.back.web7_9_codecrete_be.domain.concerts.dto.ticketOffice.TicketOfficeElement;
import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import com.back.web7_9_codecrete_be.domain.concerts.entity.ConcertLike;
import com.back.web7_9_codecrete_be.domain.concerts.entity.ConcertPlace;
import com.back.web7_9_codecrete_be.domain.concerts.entity.TicketOffice;
import com.back.web7_9_codecrete_be.domain.concerts.repository.ConcertLikeRepository;
import com.back.web7_9_codecrete_be.domain.concerts.repository.ConcertPlaceRepository;
import com.back.web7_9_codecrete_be.domain.concerts.repository.ConcertRepository;
import com.back.web7_9_codecrete_be.domain.concerts.repository.TicketOfficeRepository;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.domain.users.repository.UserRepository;
import com.back.web7_9_codecrete_be.global.error.code.AuthErrorCode;
import com.back.web7_9_codecrete_be.global.error.code.ErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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

    private final UserRepository userRepository;

    public List<ConcertItem> getConcertsList(Pageable pageable) {
        return concertRepository.getConcertItems(pageable);
    }

    public List<ConcertItem> getUpcomingConcertsList(Pageable pageable) {
        return concertRepository.getUpComingConcertItems(pageable, LocalDate.now());
    }

    public ConcertDetailResponse getConcertDetail(long concertId) {
        return concertRepository.getConcertDetailById(concertId);
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


    public void likeConcert(long concertId, long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        Concert concert = concertRepository.findById(concertId).orElseThrow();
        ConcertLike concertLike = new ConcertLike(concert, user);
        concertLikeRepository.save(concertLike);
    }

    public void dislikeConcert(long concertId, long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        Concert concert = concertRepository.findById(concertId).orElseThrow();
        ConcertLike concertLike = concertLikeRepository.findConcertLikeByConcertAndUser(concert, user);
        concertLikeRepository.delete(concertLike);
    }

    public ConcertItem updateConcert(long concertId, ConcertUpdateRequest concertUpdateRequest) {
        Concert concert = concertRepository.findById(concertId).orElseThrow();
        ConcertPlace concertPlace = concertPlaceRepository.findById(concertUpdateRequest.getPlaceId()).orElseThrow();
        concert.update(concertUpdateRequest, concertPlace);
        Concert updatedConcert = concertRepository.save(concert);
        return new ConcertItem(updatedConcert);
    }

    public void deleteConcert(long concertId) {
        Concert concert = concertRepository.findById(concertId).orElseThrow();
        concertRepository.deleteById(concertId);
    }


}
