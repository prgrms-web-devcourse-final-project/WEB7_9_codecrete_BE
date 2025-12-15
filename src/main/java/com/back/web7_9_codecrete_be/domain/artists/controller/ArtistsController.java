package com.back.web7_9_codecrete_be.domain.artists.controller;

import com.back.web7_9_codecrete_be.domain.artists.service.ArtistService;
import com.back.web7_9_codecrete_be.domain.artists.service.ArtistEnrichService;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/artists")
@RequiredArgsConstructor
@Tag(name = "Artists", description = "공연에 대한 정보를 제공하는 API 입니다.")
public class ArtistsController {
    private final ArtistService artistService;
    private final ArtistEnrichService enrichService;

    @Operation(summary = "아티스트 저장", description = "임의의 가수 300명(or 팀)을 DB에 저장합니다.")
    @GetMapping("/saved")
    public RsData<Integer> saveArtist() {
        int saved = artistService.setArtist();
        return RsData.success("아티스트 저장에 성공하였습니다.", saved);
    }

    @Operation(summary = "아티스트 정보 보완", description = "아티스트 한국어 이름, 그룹 여부, 소속 그룹 정보 보완")
    @PostMapping("/enrich")
    public RsData<Integer> enrich(
            @RequestParam(required = false, defaultValue = "100") int limit
    ) {
        int updated = enrichService.enrichArtist(limit);
        return RsData.success("enrich 성공", updated);
    }
}
