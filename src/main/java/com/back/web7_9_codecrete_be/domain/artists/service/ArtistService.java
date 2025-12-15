package com.back.web7_9_codecrete_be.domain.artists.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArtistService {

    private final SpotifyService spotifyService;

    @Transactional
    public int setArtist() {
        return spotifyService.seedKoreanArtists300();
    }
}
