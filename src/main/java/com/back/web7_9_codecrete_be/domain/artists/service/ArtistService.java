package com.back.web7_9_codecrete_be.domain.artists.service;

import com.back.web7_9_codecrete_be.domain.artists.entity.ArtistSort;
import com.back.web7_9_codecrete_be.domain.artists.dto.request.UpdateRequest;
import com.back.web7_9_codecrete_be.domain.artists.dto.response.ArtistListResponse;
import com.back.web7_9_codecrete_be.domain.artists.dto.response.ArtistDetailResponse;
import com.back.web7_9_codecrete_be.domain.artists.dto.response.SearchResponse;
import com.back.web7_9_codecrete_be.domain.artists.entity.*;
import com.back.web7_9_codecrete_be.domain.artists.repository.ArtistRepository;
import com.back.web7_9_codecrete_be.domain.artists.repository.ArtistLikeRepository;
import com.back.web7_9_codecrete_be.domain.artists.repository.ConcertArtistRepository;
import com.back.web7_9_codecrete_be.domain.artists.dto.response.ConcertListByArtistResponse;
import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import com.back.web7_9_codecrete_be.domain.concerts.repository.ConcertRepository;
import com.back.web7_9_codecrete_be.domain.concerts.service.ConcertService;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.global.error.code.ArtistErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import lombok.AccessLevel;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ArtistService {

    private final SpotifyService spotifyService;
    private final ArtistRepository artistRepository;
    private final GenreService genreService;
    private final ArtistLikeRepository artistLikeRepository;
    private final ConcertArtistRepository concertArtistRepository;
    private final ConcertRepository concertRepository;
    private final ConcertService concertService;

    @Transactional(readOnly = true)
    public Artist findArtist(Long artistId) {
        return artistRepository.findById(artistId)
                .orElseThrow(() -> new BusinessException(ArtistErrorCode.ARTIST_NOT_FOUND));
    }

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
    public Slice<ArtistListResponse> listArtist(Pageable pageable, User user, ArtistSort sort) {
        // Pageable의 sort를 제거 (우리가 정의한 sort 파라미터만 사용)
        Pageable pageableWithoutSort = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        // 로그인한 유저가 좋아요한 아티스트 ID 목록 조회
        Set<Long> likedArtistIds = new HashSet<>();
        if (user != null) {
            List<Long> artistIds = artistLikeRepository.findArtistIdsByUserId(user.getId());
            likedArtistIds.addAll(artistIds);
        }

        final Set<Long> finalLikedArtistIds = likedArtistIds;

        // 정렬에 따라 다른 쿼리 사용
        Slice<Artist> artistSlice;
        if (sort == null) {
            // sort가 없으면 기본 정렬 (id 순)
            artistSlice = artistRepository.findAllBy(pageableWithoutSort);
        } else {
            artistSlice = switch (sort) {
                case NAME -> artistRepository.findAllOrderByName(pageableWithoutSort);
                case LIKE -> artistRepository.findAllOrderByLikeCountDesc(pageableWithoutSort);
            };
        }

        return artistSlice.map(artist -> {
            boolean isLiked = finalLikedArtistIds.contains(artist.getId());
            return ArtistListResponse.from(artist, isLiked);
        });
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
            throw new BusinessException(ArtistErrorCode.INVALID_UPDATE_REQUEST);
        }
    }

    @Transactional
    public void delete(Long id) {
        Artist artist = artistRepository.findById(id)
                        .orElseThrow(() -> new BusinessException(ArtistErrorCode.ARTIST_NOT_FOUND));
        artistRepository.delete(artist);
    }

    @Transactional(readOnly = true)
    public List<SearchResponse> search(String artistName) {
        List<Artist> artists =
                artistRepository.findAllByArtistNameContainingIgnoreCaseOrNameKoContainingIgnoreCase(artistName, artistName);

        if (artists.isEmpty()) {
            throw new BusinessException(ArtistErrorCode.ARTIST_NOT_FOUND);
        }

        return artists.stream()
                .map(SearchResponse::from)
                .toList();
    }

    @Transactional
    public void likeArtist(Long artistId, User user) {
        Artist artist = findArtist(artistId);
        if(artistLikeRepository.existsByArtistAndUser(artist, user)) {
            throw new BusinessException(ArtistErrorCode.LIKES_ALREADY_EXISTS);
        }
        artistLikeRepository.save(new ArtistLike(artist, user));
        artist.increaseLikeCount();
    }

    @Transactional
    public void deleteLikeArtist(Long artistId, User user) {
        Artist artist = findArtist(artistId);
        ArtistLike likes = artistLikeRepository.findByArtistAndUser(artist, user)
                .orElseThrow(() -> new BusinessException(ArtistErrorCode.LIKES_NOT_FOUND));
        artistLikeRepository.delete(likes);
        artist.decreaseLikeCount();
    }

    @Transactional
    public void linkArtistConcert(Long artistId, Long concertId) {
        Artist artist = findArtist(artistId);
        // TODO: 멘토링 질문 남겨놓은 기능이라, 멘토링 후 구현 방향 확정되면 함수 선언 후 Service 사용 예정. 현재는 임시로 Repository 사용
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow();
        concertArtistRepository.save(new ConcertArtist(artist, concert));
    }

    @Transactional(readOnly = true)
    public List<ConcertListByArtistResponse> getConcertList(Long userId) {
        List<Long> artistIds =
                artistLikeRepository.findArtistIdsByUserId(userId);

        // 찜한 아티스트가 없는 경우 빈 배열 반환 -> 예외 처리(Error 던지기) 안 함
        if (artistIds.isEmpty()) {
            return List.of();
        }
        List<Concert> concerts = concertService.findConcertsByArtistIds(artistIds);
        return concerts.stream()
                .map(ConcertListByArtistResponse::from)
                .toList();
    }


}
