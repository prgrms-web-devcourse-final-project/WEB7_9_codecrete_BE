package com.back.web7_9_codecrete_be.domain.artists.service;

import com.back.web7_9_codecrete_be.domain.artists.entity.Artist;
import com.back.web7_9_codecrete_be.domain.artists.entity.Genre;
import com.back.web7_9_codecrete_be.domain.artists.repository.ArtistRepository;
import com.back.web7_9_codecrete_be.domain.artists.repository.GenreRepository;
import com.back.web7_9_codecrete_be.global.error.code.ArtistErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArtistService {

    private final SpotifyService spotifyService;
    private final ArtistRepository artistRepository;
    private final GenreService genreService;

    @Transactional
    public int setArtist() {
        return spotifyService.seedKoreanArtists300();
    }

    @Transactional
    public Artist createArtist(String artistName, String artistGroup, String artistType, String genreName) {
        Genre genre = genreService.findByGenreName(genreName);
        if(!artistRepository.existsByArtistName(artistName) || !artistRepository.existsByNameKo(artistName)) {
            throw new BusinessException(ArtistErrorCode.ARTIST_ALREADY_EXISTS);
        }
        Artist artist = new Artist(artistName, artistGroup, artistType, genre);
        artistRepository.save(artist);
        return artist;
    }

}
