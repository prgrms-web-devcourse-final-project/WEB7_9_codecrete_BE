package com.back.web7_9_codecrete_be.domain.concerts.controller;

import com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concert.ConcertListResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concertPlace.ConcertPlaceListResponse;
import com.back.web7_9_codecrete_be.domain.concerts.service.ConcertService;
import com.back.web7_9_codecrete_be.domain.concerts.service.KopisApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/concerts/")
@Controller
@RequiredArgsConstructor
public class ConcertController {
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

}
