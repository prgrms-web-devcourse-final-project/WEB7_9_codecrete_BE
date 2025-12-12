package com.back.web7_9_codecrete_be.domain.artists.controller;

import com.back.web7_9_codecrete_be.domain.artists.service.ArtistService;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/artists")
@RequiredArgsConstructor
public class ArtistsController {
    private final ArtistService artistService;

    @GetMapping("/saved")
    public RsData<Integer> saveArtist() {
        int saved = artistService.setArtist();
        return RsData.success("아티스트 저장에 성공하였습니다.", saved);
    }
}
