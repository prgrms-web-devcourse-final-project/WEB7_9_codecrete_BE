package com.back.web7_9_codecrete_be.domain.artists.controller;

import com.back.web7_9_codecrete_be.domain.artists.dto.response.GenreResponse;
import com.back.web7_9_codecrete_be.domain.artists.service.GenreService;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/genre")
@RequiredArgsConstructor
public class GenreController {

    private final GenreService genreService;

    @GetMapping()
    public RsData<List<GenreResponse>> genreList() {
        return RsData.success("전체 장르 조회 성공", genreService.genreList());
    }
}
