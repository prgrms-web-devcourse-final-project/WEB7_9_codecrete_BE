package com.back.web7_9_codecrete_be.domain.artists.controller;

import com.back.web7_9_codecrete_be.domain.artists.dto.request.CreateRequest;
import com.back.web7_9_codecrete_be.domain.artists.dto.request.UpdateRequest;
import com.back.web7_9_codecrete_be.domain.artists.service.ArtistService;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/artists")
@RequiredArgsConstructor
@Tag(name = "ArtistAdmin", description = "아티스트 관리자 API 입니다.")
public class ArtistAdminController {

    private final ArtistService artistService;

    @Operation(summary = "아티스트 생성", description = "아티스트를 등록합니다.")
    @PostMapping()
    public RsData<Void> create(
            @Valid @RequestBody CreateRequest reqBody
    ) {
        artistService.createArtist(reqBody.spotifyId(), reqBody.artistName(), reqBody.artistGroup(), reqBody.artistType(), reqBody.genreName());
        return RsData.success("아티스트 생성이 완료되었습니다.", null);
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

}
