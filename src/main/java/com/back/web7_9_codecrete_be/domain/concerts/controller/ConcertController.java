package com.back.web7_9_codecrete_be.domain.concerts.controller;

import com.back.web7_9_codecrete_be.domain.concerts.service.ConcertService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/concerts/")
@Controller
@RequiredArgsConstructor
public class ConcertController {
    private final ConcertService concertService;



}
