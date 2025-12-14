package com.back.web7_9_codecrete_be.domain.concerts.controller;

import com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concert.ConcertListResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concertPlace.ConcertPlaceListResponse;
import com.back.web7_9_codecrete_be.domain.concerts.service.KopisApiService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/concerts/")
@Tag(name = "Concerts Admin", description = "공연에 대해서 관리하는 API입니다. ")
public class ConcertAdminController {
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


}
