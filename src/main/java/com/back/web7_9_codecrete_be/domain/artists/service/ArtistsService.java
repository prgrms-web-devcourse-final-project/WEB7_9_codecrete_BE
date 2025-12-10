package com.back.web7_9_codecrete_be.domain.artists.service;

import com.back.web7_9_codecrete_be.domain.artists.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArtistsService {
    private final ArtistRepository artistRepository;
    private final ArtistLikeRepository artistLikeRepository;
    private final ConcertArtistRepository concertArtistRepository;
    private final GenreLikeRepository genreLikeRepository;
    private final GenreRepository genreRepository;
}
