package com.back.web7_9_codecrete_be.domain.artists.dto.response;

import java.util.List;

// Spotify 아티스트 상세 정보 캐시용 DTO - Redis에 저장하기 위한 데이터 구조

public record SpotifyArtistDetailCache(
        // 아티스트 기본 정보
        String artistName,
        String profileImageUrl,
        double popularity,
        
        // Top Tracks (상위 10개)
        List<TopTrackResponse> topTracks,
        
        // 앨범 목록 (최대 20개)
        List<AlbumResponse> albums,
        int totalAlbums
) {
}

