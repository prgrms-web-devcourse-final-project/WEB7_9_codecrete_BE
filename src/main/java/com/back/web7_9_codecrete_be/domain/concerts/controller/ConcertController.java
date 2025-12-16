package com.back.web7_9_codecrete_be.domain.concerts.controller;

import com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concert.ConcertListResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concertPlace.ConcertPlaceListResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertDetailResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertItem;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertLikeResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.ticketOffice.TicketOfficeElement;
import com.back.web7_9_codecrete_be.domain.concerts.entity.TicketOffice;
import com.back.web7_9_codecrete_be.domain.concerts.service.ConcertService;
import com.back.web7_9_codecrete_be.domain.concerts.service.KopisApiService;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.global.rq.Rq;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.swing.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("api/v1/concerts/")
@RequiredArgsConstructor
@Tag(name = "Concerts", description = "공연에 대한 정보를 제공하는 API 입니다.")
public class ConcertController {
    private final ConcertService concertService;
    private final Rq rq;

    @Operation(summary = "공연목록", description = "공연 전체 목록을 조회합니다. 시작일자를 기준으로 오름차순 조회합니다.")
    @GetMapping("list")
    public RsData<List<ConcertItem>> getList (
            @Schema(description = "페이징 처리 또는 무한 스크롤 구현에 쓸 Pageable 객체입니다.")
            Pageable pageable
    ) {
        return RsData.success(concertService.getConcertsList(pageable));
    }

    @Operation(summary = "다가오는 공연 목록", description = "오늘을 기준으로 다가오는 공연 목록을 조회합니다.")
    @GetMapping("upComingList")
    public RsData<List<ConcertItem>> getUpComingList (
            @Schema(description = "페이징 처리 또는 무한 스크롤 구현에 쓸 Pageable 객체입니다.")
            Pageable pageable
    ) {
      return RsData.success(concertService.getUpcomingConcertsList(pageable));
    }

    // todo: 내용 구현 필요
    @Operation(summary = "공연 예매일 기준 조회(구현 전)", description = "현 시간을 기준으로 예매시간을 내림차순으로 출력하는 공연 목록을 조회합니다.")
    @GetMapping("upComingTicketingList")
    public RsData<List<ConcertItem>> getUpComingTicketingList (
            @Schema(description = "페이징 처리 또는 무한 스크롤 구현에 사용할 Pageable 객체입니다.")
            Pageable pageable
    ){
        return null;
    }

    @Operation(summary = "좋아요 한 공연 조회", description = "좋아요를 누른 공연에 대한 목록을 조회합니다. 저장 날짜를 기준으로 내림차순 정렬로 표시합니다.(최신으로 추가된 목록순입니다.)")
    @GetMapping("likedConcertList")
    public RsData<List<ConcertItem>> getLikedConcertList (
            @Schema(description = "페이징 처리 또는 무한 스크롤 구현에 쓸 Pageable 객체입니다.")
            Pageable pageable
    ){
        User user = rq.getUser();
        return RsData.success(concertService.getLikedConcertsList(pageable,user));
    }

    @Operation(summary = "공연 상세 조회", description = "공연에 대한 상세 목록을 조회합니다.")
    @GetMapping("concertDetail")
    public ConcertDetailResponse getConcertDetail(
            @RequestParam
            @Schema(description = "조회 기준이 되는 concertId입니다. ?concertId={concertId} 로 값을 넘기시면 됩니다.")
            long concertId
    ) {
        return concertService.getConcertDetail(concertId);
    }

    @Operation(summary = "공연 예매처 조회", description = "공연에 대한 예매처들을 조회합니다.")
    @GetMapping("ticketOffices")
    public RsData<List<TicketOfficeElement>> getTicketOffices (
            @RequestParam
            @Schema(description = "조회 기준이 되는 concertId입니다. ?concertId={concertId} 로 값을 넘기시면 됩니다.")
            long concertId
    ){
        return RsData.success(concertService.getTicketOfficesList(concertId));
    }

    @Operation(summary = "공연 좋아요 기능", description = "사용자가 마음에 드는 공연에 대해 좋아요를 통해 저장할 수 있습니다.")
    @PostMapping("like/{concertId}")
    public RsData<Void> likeConcert(
            @PathVariable long concertId
    ) {
        User user = rq.getUser();
        concertService.likeConcert(concertId, user);
        return RsData.success(null);
    }

    @Operation(summary = "공연 좋아요 해제 기능", description = "좋아요를 해제할 수 있습니다.")
    @DeleteMapping("dislike/{concertId}")
    public RsData<Void> dislikeConcert(
            @PathVariable long concertId
    ) {
        User user = rq.getUser();
        concertService.dislikeConcert(concertId, user);
        return RsData.success(null);
    }

    @Operation(summary = "공연 좋아요 여부 확인", description = "좋아요 여부를 확인합니다.")
    @GetMapping("isLike/{concertId}")
    public RsData<ConcertLikeResponse> isLikeConcert(
            @PathVariable long concertId
    ){
        User user = rq.getUser();
        return RsData.success(concertService.isLikeConcert(concertId, user));
    }

    // todo : 제목으로 만 검색 기능 구현 -> 추후 아티스트 정보랑 연동 <- 중요 / 정렬 기준? 최신등록순 정렬
    @Operation(summary = "공연 검색", description = "제목에 키워드를 포함하고 있는 공연 정보를 검색합니다.")
    @GetMapping("search")
    public RsData<List<ConcertItem>> searchConcert(
            @Schema(description = "공연 정보 검색 키워드입니다.")
            @RequestParam String keyword,
            @Schema(description = "페이징 처리 또는 무한 스크롤 구현에 쓸 Pageable 객체입니다.")
            Pageable pageable

    ){
        return RsData.success(concertService.getConcertListByKeyword(keyword,pageable));
    }

}
