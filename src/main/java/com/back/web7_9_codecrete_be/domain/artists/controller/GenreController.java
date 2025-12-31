package com.back.web7_9_codecrete_be.domain.artists.controller;

import com.back.web7_9_codecrete_be.domain.artists.dto.response.GenreResponse;
import com.back.web7_9_codecrete_be.domain.artists.service.GenreService;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/genre")
@RequiredArgsConstructor
@Tag(name = "Genre", description = "장르 관련 API 입니다.")
public class GenreController {

    private final GenreService genreService;

    @Operation(summary = "전체 장르 목록", description = "DB에 저장되어있는 전체 장르 목록을 반환합니다.")
    @GetMapping()
    public RsData<List<GenreResponse>> genreList() {
        return RsData.success("전체 장르 조회 성공", genreService.genreList());
    }
}
