package com.back.web7_9_codecrete_be.domain.artists.service;

import com.back.web7_9_codecrete_be.domain.artists.dto.request.UpdateRequest;
import com.back.web7_9_codecrete_be.domain.artists.dto.response.ArtistListResponse;
import com.back.web7_9_codecrete_be.domain.artists.dto.response.ArtistDetailResponse;
import com.back.web7_9_codecrete_be.domain.artists.entity.Artist;
import com.back.web7_9_codecrete_be.domain.artists.entity.ArtistType;
import com.back.web7_9_codecrete_be.domain.artists.entity.Genre;
import com.back.web7_9_codecrete_be.domain.artists.repository.ArtistRepository;
import com.back.web7_9_codecrete_be.domain.artists.repository.ArtistLikeRepository;
import com.back.web7_9_codecrete_be.global.error.code.ArtistErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArtistService {

    private final SpotifyService spotifyService;
    private final ArtistRepository artistRepository;
    private final GenreService genreService;
    private final ArtistLikeRepository artistLikeRepository;

    @Transactional
    public int setArtist() {
        return spotifyService.seedKoreanArtists300();
    }

    @Transactional
    public Artist createArtist(String artistName, String artistGroup, ArtistType artistType, String genreName) {
        Genre genre = genreService.findByGenreName(genreName);
        if(artistRepository.existsByArtistName(artistName) || artistRepository.existsByNameKo(artistName)) {
            throw new BusinessException(ArtistErrorCode.ARTIST_ALREADY_EXISTS);
        }
        Artist artist = new Artist(artistName, artistGroup, artistType, genre);
        artistRepository.save(artist);
        return artist;
    }

    @Transactional(readOnly = true)
    public List<ArtistListResponse> listArtist() {
        return artistRepository.findAll().stream()
                .map(ArtistListResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ArtistDetailResponse getArtistDetail(Long artistId) {
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new BusinessException(ArtistErrorCode.ARTIST_NOT_FOUND));

        if (artist.getSpotifyArtistId() == null) {
            throw new BusinessException(ArtistErrorCode.SPOTIFY_NOT_FOUND);
        }

        long likeCount = artistLikeRepository.countByArtistId(artistId);

        return spotifyService.getArtistDetail(
                artist.getSpotifyArtistId(),
                artist.getArtistGroup(),
                artist.getArtistType(),
                likeCount,
                artist.getId(),
                artist.getGenre() != null ? artist.getGenre().getId() : null
        );
    }

    @Transactional
    public void updateArtist(Long id, UpdateRequest req) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ArtistErrorCode.ARTIST_NOT_FOUND));

        boolean changed = false;

        if (req.artistName() != null && !req.artistName().isBlank()) {
            artist.changeName(req.artistName().trim());
            changed = true;
        }

        if (req.artistGroup() != null && !req.artistGroup().isBlank()) {
            artist.changeGroup(req.artistGroup().trim());
            changed = true;
        }

        if (req.artistType() != null) {
            artist.changeType(req.artistType());
            changed = true;
        }

        if (req.genreName() != null && !req.genreName().isBlank()) {
            Genre genre = genreService.findByGenreName(req.genreName().trim());
            artist.changeGenre(genre);
            changed = true;
        }

        if (!changed) {
            throw new BusinessException(ArtistErrorCode.INVALID_UPDATE_REQUEST); // "수정할 값이 없습니다"
        }
    }

    @Transactional
    public void delete(Long id) {
        Artist artist = artistRepository.findById(id)
                        .orElseThrow(() -> new BusinessException(ArtistErrorCode.ARTIST_NOT_FOUND));
        artistRepository.delete(artist);
    }

}
