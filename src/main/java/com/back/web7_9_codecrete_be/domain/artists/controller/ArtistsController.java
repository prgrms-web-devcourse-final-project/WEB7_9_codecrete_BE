package com.back.web7_9_codecrete_be.domain.artists.controller;

import com.back.web7_9_codecrete_be.domain.artists.entity.ArtistSort;
import com.back.web7_9_codecrete_be.domain.artists.dto.request.CreateRequest;
import com.back.web7_9_codecrete_be.domain.artists.dto.request.SearchRequest;
import com.back.web7_9_codecrete_be.domain.artists.dto.request.UpdateRequest;
import com.back.web7_9_codecrete_be.domain.artists.dto.response.ArtistListResponse;
import com.back.web7_9_codecrete_be.domain.artists.dto.response.ArtistDetailResponse;
import com.back.web7_9_codecrete_be.domain.artists.dto.response.ConcertListByArtistResponse;
import com.back.web7_9_codecrete_be.domain.artists.dto.response.SearchResponse;
import com.back.web7_9_codecrete_be.domain.artists.service.ArtistService;
import com.back.web7_9_codecrete_be.domain.artists.service.ArtistEnrichService;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.global.rq.Rq;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/artists")
@RequiredArgsConstructor
@Tag(name = "Artists", description = "아티스트에 대한 정보를 제공하는 API 입니다.")
public class ArtistsController {
    private final ArtistService artistService;
    private final ArtistEnrichService enrichService;
    private final Rq rq;

    @Operation(summary = "아티스트 저장", description = "임의의 가수(or 팀)을 DB에 저장합니다.")
    @PostMapping("/saved")
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

    @Operation(summary = "MusicBrainz ID 수집", description = "아티스트의 MusicBrainz ID를 수집합니다.")
    @PostMapping("/musicbrainz-id")
    public RsData<Integer> fetchMusicBrainzIds(
            @RequestParam(required = false, defaultValue = "100") int limit
    ) {
        int updated = enrichService.fetchMusicBrainzIds(limit);
        return RsData.success("MusicBrainz ID 수집 성공", updated);
    }

    @Operation(summary = "아티스트 생성", description = "아티스트를 등록합니다.")
    @PostMapping()
    public RsData<Void> create(
            @Valid @RequestBody CreateRequest reqBody
    ) {
        artistService.createArtist(reqBody.spotifyID(), reqBody.artistName(), reqBody.artistGroup(), reqBody.artistType(), reqBody.genreName());
        return RsData.success("아티스트 생성이 완료되었습니다.", null);
    }

    @Operation(summary = "아티스트 목록 조회",
            description = "아티스트 전체 목록을 조회합니다(NAME: 이름순 / LIKE: 인기순 (좋아요 많은 순)")
    @GetMapping()
    public RsData<Slice<ArtistListResponse>> list(
            Pageable pageable,
            @RequestParam(required = false) ArtistSort sort
    ) {
        User user = rq.getUserOrNull(); // 로그인하지 않은 경우 null
        return RsData.success("아티스트 전체 목록을 조회했습니다.", artistService.listArtist(pageable, user, sort));
    }

    @Operation(summary = "아티스트 상세 조회", description = "아티스트의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public RsData<ArtistDetailResponse> artist(
            @PathVariable Long id
    ) {
        User user = rq.getUserOrNull(); // 로그인하지 않은 경우 null
        return RsData.success("아티스트 상세 조회를 성공했습니다.", artistService.getArtistDetail(id, user));
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
        User user = rq.getUser();
        artistService.likeArtist(id, user);
        return RsData.success("아티스트 찜 성공", null);
    }

    @Operation(summary = "아티스트 찜 해체", description = "id 에 해당하는 아티스트에게 등록했던 찜을 해제합니다.")
    @DeleteMapping("/likes/{id}")
    public RsData<Void> deleteArtistLikes(
            @PathVariable Long id
    ) {
        User user = rq.getUser();
        artistService.deleteLikeArtist(id, user);
        return RsData.success("아티스트 찜 해제 성공", null);
    }

    @Operation(summary = "아티스트 공연 기록 저장", description = "아티스트 id 와 공연 id 를 받아 해당 아티스트의 공연 기록을 저장합니다.")
    @PostMapping("/link/{artistId}/{concertId}")
    public RsData<Void> saveConcertArtist(
            @PathVariable Long artistId,
            @PathVariable Long concertId
    ) {
        artistService.linkArtistConcert(artistId, concertId);
        return RsData.success("아티스트 공연 기록 저장 성공", null);
    }

    @Operation(summary = "개인화된 공연 리스트 생성", description = "유저가 찜한 아티스트들의 공연 리스트를 조회합니다. 로그인한 유저만 가능합니다.")
    @GetMapping("/list")
    public RsData<List<ConcertListByArtistResponse>> concertList() {
        User user = rq.getUser();
        return RsData.success("찜한 아티스트 공연 리스트 조회 성공", artistService.getConcertList(user.getId()));
    }

    @Operation(summary = "아티스트 인기 순위(구현 전)", description = "Spotify 인기도를 바탕으로 아티스트 인기 순위 랭킹을 제공합니다.")
    @GetMapping("/ranking")
    public void artistRanking() {}

    @Operation(summary = "장르 기반 아티스트 추천(구현 전)", description = "찜한 장르를 기반으로 아티스트 추천 리스트를 제공합니다.")
    @GetMapping("/recommendation/{genreId}")
    public void recommendArtist(
            @PathVariable Long genreId
    ) {}

    @Operation(summary = "공연 셋리스트 생성(구현 전)", description = "사용자가 공연 셋리스트를 생성합니다.")
    @PostMapping("/setlist/{concertId}/{artistId}")
    public void makeSetlist(
            @PathVariable Long concertId,
            @PathVariable Long artistId
    ) {}

    @Operation(summary = "공연 셋리스트 조회(구현 전)", description = "다른 사용자들이 생성한 셋리스트를 조회합니다.")
    @GetMapping("/setlist/{concertId}/{artistId}")
    public void getSetlist(
            @PathVariable Long concertId,
            @PathVariable Long artistId
    ) {}
}

