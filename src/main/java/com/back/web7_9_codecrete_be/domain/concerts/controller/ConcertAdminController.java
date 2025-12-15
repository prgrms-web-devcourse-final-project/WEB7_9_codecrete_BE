package com.back.web7_9_codecrete_be.domain.concerts.controller;

import com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concert.ConcertListResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concertPlace.ConcertPlaceListResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertItem;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertUpdateRequest;
import com.back.web7_9_codecrete_be.domain.concerts.service.ConcertService;
import com.back.web7_9_codecrete_be.domain.concerts.service.KopisApiService;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/concerts/")
@Tag(name = "Concerts Admin", description = "공연에 대해서 관리하는 API입니다. ")
public class ConcertAdminController { // todo : 인증 권한 추가하기
    private final ConcertService concertService;
    private final KopisApiService kopisApiService;

    @GetMapping("tests")
    public ConcertListResponse tests() {
        return kopisApiService.getConcertsList();
    }

    @GetMapping("totalGetTest")
    public ConcertListResponse totalGetTest() throws InterruptedException {
        return kopisApiService.setConcertsList();
    }

    @GetMapping("setConcertPlace")
    public ConcertPlaceListResponse setConcertPlace() throws InterruptedException {
        return kopisApiService.setConcertPlace();
    }

    @PatchMapping("updateConcert/{concertId}")
    public RsData<ConcertItem> updateConcert(
            @PathVariable Long concertId,
            @RequestBody ConcertUpdateRequest concertUpdateRequest
    ){
        ConcertItem concertItem = concertService.updateConcert(concertId, concertUpdateRequest);
        return RsData.success("공연 정보 수정이 완료되었습니다.",concertItem);
    }

    @DeleteMapping("deleteConcert/{concertId}")
    public RsData<Void> deleteConcert(@PathVariable Long concertId){
        concertService.deleteConcert(concertId);
        return RsData.success("공연 정보 삭제에 성공하였습니다.",null);
    }
}
