package com.back.web7_9_codecrete_be.domain.artists.controller;

import com.back.web7_9_codecrete_be.domain.artists.dto.request.CreateRequest;
import com.back.web7_9_codecrete_be.domain.artists.dto.request.SearchRequest;
import com.back.web7_9_codecrete_be.domain.artists.dto.request.UpdateRequest;
import com.back.web7_9_codecrete_be.domain.artists.dto.response.ArtistListResponse;
import com.back.web7_9_codecrete_be.domain.artists.dto.response.ArtistDetailResponse;
import com.back.web7_9_codecrete_be.domain.artists.dto.response.SearchResponse;
import com.back.web7_9_codecrete_be.domain.artists.service.ArtistService;
import com.back.web7_9_codecrete_be.domain.artists.service.ArtistEnrichService;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/artists")
@RequiredArgsConstructor
@Tag(name = "Artists", description = "아티스트에 대한 정보를 제공하는 API 입니다.")
public class ArtistsController {
    private final ArtistService artistService;
    private final ArtistEnrichService enrichService;

    @Operation(summary = "아티스트 저장", description = "임의의 가수(or 팀)을 DB에 저장합니다.")
    @GetMapping("/saved")
    public RsData<Integer> saveArtist() {
        int saved = artistService.setArtist();
        return RsData.success("아티스트 저장에 성공하였습니다.", saved);
    }

    @Operation(summary = "아티스트 정보 보완", description = "아티스트 한국어 이름, 그룹 여부, 소속 그룹 정보를 보완합니다.")
    @PostMapping("/enrich")
    public RsData<Integer> enrich(
            @RequestParam(required = false, defaultValue = "100") int limit
    ) {
        int updated = enrichService.enrichArtist(limit);
        return RsData.success("enrich 성공", updated);
    }

    @Operation(summary = "아티스트 생성", description = "아티스트를 등록합니다.")
    @PostMapping()
    public RsData<Void> create(
            @Valid @RequestBody CreateRequest reqBody
    ) {
        artistService.createArtist(reqBody.artistName(), reqBody.artistGroup(), reqBody.artistType(), reqBody.genreName());
        return RsData.success("아티스트 생성이 완료되었습니다.", null);
    }

    @Operation(summary = "아티스트 목록 조회", description = "아티스트 전체 목록을 조회합니다.")
    @GetMapping()
    public RsData<List<ArtistListResponse>> list() {
        return RsData.success("아티스트 전체 목록을 조회했습니다.", artistService.listArtist());
    }

    @Operation(summary = "아티스트 상세 조회", description = "아티스트의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public RsData<ArtistDetailResponse> artist(
            @PathVariable Long id
    ) {
        return RsData.success("아티스트 상세 조회를 성공했습니다.", artistService.getArtistDetail(id));
    }

    @Operation(summary = "아티스트 정보 수정", description = "아티스트 정보를 수정합니다.")
    @PatchMapping("/{id}")
    public RsData<Void> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRequest reqBody
    ) {
        artistService.updateArtist(id, reqBody);
        return RsData.success("아티스트 정보 수정을 완료했습니다.", null);
    }

    @Operation(summary = "아티스트 정보 삭제", description = "아티스트 정보를 삭제합니다.")
    @DeleteMapping("/{id}")
    public RsData<Void> delete(
            @PathVariable Long id
    ) {
        artistService.delete(id);
        return RsData.success("아티스트 정보를 삭제했습니다.", null);
    }

    @Operation(summary = "아티스트 검색",
            description = "아티스트 이름 또는 키워드를 입력하면, 해당 키워드가 포함된 아티스트 목록 또는 이름에 해당하는 아티스트를 조회합니다.")
    @PostMapping("/search")
    public RsData<List<SearchResponse>> search(
            @Valid @RequestBody SearchRequest reqBody
    ) {
        return RsData.success("아티스트 검색에 성공했습니다.", artistService.search(reqBody.artistName()));
    }

    @Operation(summary = "아티스트 찜하기", description = "id 에 해당하는 특정 아티스트를 찜합니다. 로그인 상태에서만 가능합니다.")
    @PostMapping("/likes/{id}")
    public RsData<Void> artistLikes(
            @PathVariable Long id
    ) {
        artistService.likeArtist(id);
        return RsData.success("아티스트 찜 성공", null);
    }

    @Operation(summary = "아티스트 찜 해체", description = "id 에 해당하는 아티스트에게 등록했던 찜을 해제합니다.")
    @DeleteMapping("/likes/{id}")
    public RsData<Void> deleteArtistLikes(
            @PathVariable Long id
    ) {
        artistService.deleteLikeArtist(id);
        return RsData.success("아티스트 찜 해제 성공", null);
    }

    @Operation(summary = "개인화된 공연 리스트 생성", description = "유저가 찜한 아티스트를 기반으로 공연 리스트를 생성합니다.")
    @PostMapping("/list")
    public void concertList() {}

    @Operation(summary = "아티스트 인기 순위", description = "Spotify 인기도를 바탕으로 아티스트 인기 순위 랭킹을 제공합니다.")
    @GetMapping("/ranking")
    public void artistRanking() {}

    @Operation(summary = "장르 기반 아티스트 추천", description = "찜한 장르를 기반으로 아티스트 추천 리스트를 제공합니다.")
    @GetMapping("/recommendation/{genreId}")
    public void recommendArtist(
            @PathVariable Long genreId
    ) {}

    @Operation(summary = "공연 셋리스트 생성", description = "사용자가 공연 셋리스트를 생성합니다.")
    @PostMapping("/setlist/{concertId}/{artistId}")
    public void makeSetlist(
            @PathVariable Long concertId,
            @PathVariable Long artistId
    ) {}

    @Operation(summary = "공연 셋리스트 조회", description = "다른 사용자들이 생성한 셋리스트를 조회합니다.")
    @GetMapping("/setlist/{concertId}/{artistId}")
    public void getSetlist(
            @PathVariable Long concertId,
            @PathVariable Long artistId
    ) {}
}

