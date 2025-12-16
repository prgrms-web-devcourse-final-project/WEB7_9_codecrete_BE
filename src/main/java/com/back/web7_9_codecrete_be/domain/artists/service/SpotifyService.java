package com.back.web7_9_codecrete_be.domain.artists.service;

import com.back.web7_9_codecrete_be.domain.artists.dto.response.AlbumResponse;
import com.back.web7_9_codecrete_be.domain.artists.dto.response.ArtistDetailResponse;
import com.back.web7_9_codecrete_be.domain.artists.dto.response.RelatedArtistResponse;
import com.back.web7_9_codecrete_be.domain.artists.dto.response.TopTrackResponse;
import com.back.web7_9_codecrete_be.domain.artists.entity.Genre;
import com.back.web7_9_codecrete_be.domain.artists.repository.ArtistRepository;
import com.back.web7_9_codecrete_be.domain.artists.repository.GenreRepository;
import com.back.web7_9_codecrete_be.global.error.code.ArtistErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import com.back.web7_9_codecrete_be.global.spotify.SpotifyClient;
import com.neovisionaries.i18n.CountryCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.enums.AlbumType;
import se.michaelthelin.spotify.exceptions.detailed.NotFoundException;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.Artist;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotifyService {

    private final ArtistRepository artistRepository;
    private final GenreRepository genreRepository;
    private final SpotifyClient spotifyClient;

    @Transactional
    public int seedKoreanArtists300() {
        try {
            SpotifyApi api = spotifyClient.getAuthorizedApi();

            final int targetCount = 300;
            final int limit = 50;
            int totalSaved = 0;

            List<String> queries = List.of(
                    "k-pop", "korean pop",
                    "BTS", "BLACKPINK", "NewJeans", "LE SSERAFIM", "aespa", "IVE", "NCT",
                    "Stray Kids", "TWICE", "Red Velvet", "IU", "태연",
                    "korean hip hop", "korean r&b", "korean ballad", "korean ost",
                    "korean indie", "korean rock", "trot"
            );

            for (String q : queries) {
                if (totalSaved >= targetCount) break;

                int offset = 0;

                while (totalSaved < targetCount) {
                    Paging<Artist> paging = api.searchArtists(q)
                                    .limit(limit)
                                    .offset(offset)
                                    .build()
                                    .execute();

                    var items = paging.getItems();
                    if (items == null || items.length == 0) break;

                    for (var spotifyArtist : items) {
                        if (totalSaved >= targetCount) break;

                        String spotifyId = spotifyArtist.getId();
                        String name = spotifyArtist.getName();

                        if (spotifyId == null || name == null || name.isBlank()) continue;
                        if (artistRepository.existsBySpotifyArtistId(spotifyId)) continue;
                        if (!isLikelyKoreanMusic(spotifyArtist)) continue;

                        String mainGenreName = pickMainGenreName(spotifyArtist);
                        Genre genre = findOrCreateGenreByName(mainGenreName, null);

                        String artistType = inferArtistType(spotifyArtist);

                        com.back.web7_9_codecrete_be.domain.artists.entity.Artist artistEntity =
                                new com.back.web7_9_codecrete_be.domain.artists.entity.Artist(
                                spotifyId,
                                name.trim(),
                                null,        // artistGroup
                                artistType,
                                genre
                        );

                        artistRepository.save(artistEntity);
                        totalSaved++;
                    }

                    offset += limit;
                    if (offset >= paging.getTotal()) break;

                    Thread.sleep(200);
                }
            }

            if (totalSaved == 0) {
                throw new BusinessException(ArtistErrorCode.ARTIST_SEED_FAILED);
            }

            return totalSaved;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ArtistErrorCode.SPOTIFY_API_ERROR);
        }
    }

    private static final List<String> KOREAN_GENRE_HINTS = List.of(
            "k-pop", "korean", "trot",
            "k-hip hop", "k-rap", "k-ballad", "k-r&b", "k-indie", "k-rock",
            "korean hip hop", "korean r&b", "korean ballad", "korean ost",
            "korean indie", "korean rock"
    );

    private boolean isLikelyKoreanMusic(Artist a) {
        String[] genres = a.getGenres();
        if (genres != null) {
            for (String g : genres) {
                if (g == null) continue;
                String s = g.toLowerCase();
                for (String hint : KOREAN_GENRE_HINTS) {
                    if (s.contains(hint)) return true;
                }
            }
        }
        String name = a.getName();
        return name != null && name.matches(".*[가-힣].*");
    }

    private String pickMainGenreName(Artist a) {
        String[] genres = a.getGenres();
        if (genres == null || genres.length == 0) return "k-pop";

        for (String g : genres) {
            if (g == null) continue;
            String s = g.toLowerCase();
            if (s.contains("trot")) return "trot";
            if (s.contains("ballad")) return "ballad";
            if (s.contains("hip hop")) return "hiphop";
            if (s.contains("r&b") || s.contains("rb")) return "rnb";
            if (s.contains("ost")) return "ost";
            if (s.contains("rock")) return "rock";
            if (s.contains("indie")) return "indie";
            if (s.contains("k-pop") || s.contains("korean")) return "k-pop";
        }
        return "k-pop";
    }

    private String inferArtistType(Artist a) {
        String[] genres = a.getGenres();
        if (genres != null) {
            for (String g : genres) {
                if (g == null) continue;
                String s = g.toLowerCase();
                if (s.contains("boy group") || s.contains("girl group")) return "GROUP";
            }
        }
        return "SOLO";
    }

    private Genre findOrCreateGenreByName(String genreName, String genreGroup) {
        return genreRepository.findByGenreName(genreName)
                .orElseGet(() -> genreRepository.save(new Genre(genreName, genreGroup, null)));
    }

    @Transactional(readOnly = true)
    public ArtistDetailResponse getArtistDetail(
            String spotifyArtistId,
            String artistGroup,
            String artistType,
            long likeCount,
            long artistId,
            Long genreId
    ) {
        try {
            SpotifyApi api = spotifyClient.getAuthorizedApi();

            Artist artist = api.getArtist(spotifyArtistId).build().execute(); // 메인 정보는 실패 시 예외 발생

            Track[] topTracks = safeGetTopTracks(api, spotifyArtistId);
            Paging<AlbumSimplified> albums = safeGetAlbums(api, spotifyArtistId);

            List<RelatedArtistResponse> relatedResponses = safeGetRelated(
                    api,
                    artist,
                    artistGroup,
                    artistId,
                    genreId
            );

            return new ArtistDetailResponse(
                    artist.getName(),
                    artistGroup,
                    artistType,
                    pickImageUrl(artist.getImages()),
                    likeCount,
                    albums != null ? albums.getTotal() : 0,
                    artist.getPopularity(), // (너가 별점으로 바꾸고 싶으면 여기 가공)
                    "", // Spotify에서는 설명을 제공하지 않아 공란 처리
                    toAlbumResponses(albums != null ? albums.getItems() : null, spotifyArtistId),
                    toTopTrackResponses(topTracks),
                    relatedResponses
            );
        } catch (Exception e) {
            log.error("Spotify 상세 조회 실패: artistId={}", spotifyArtistId, e);
            throw new BusinessException(ArtistErrorCode.SPOTIFY_API_ERROR);
        }
    }

    private Track[] safeGetTopTracks(SpotifyApi api, String artistId) {
        try {
            return api.getArtistsTopTracks(artistId, CountryCode.KR)
                    .build()
                    .execute();
        } catch (NotFoundException nf) {
            log.warn("Spotify top tracks not found: artistId={}", artistId);
            return new Track[0];
        } catch (Exception e) {
            log.warn("Spotify top tracks fetch error: artistId={}", artistId, e);
            return new Track[0];
        }
    }

    private Paging<AlbumSimplified> safeGetAlbums(SpotifyApi api, String artistId) {
        try {
            return api.getArtistsAlbums(artistId)
                    .market(CountryCode.KR)
                    .limit(20)
                    .build()
                    .execute();
        } catch (NotFoundException nf) {
            log.warn("Spotify albums not found: artistId={}", artistId);
            return null;
        } catch (Exception e) {
            log.warn("Spotify albums fetch error: artistId={}", artistId, e);
            return null;
        }
    }

    /**
     * ✅ related artists
     * - 정상 호출
     * - related가 비거나 404면 fallback(장르 기반 검색)으로 대체
     */
    private List<RelatedArtistResponse> safeGetRelated(
            SpotifyApi api,
            Artist me,
            String artistGroup,
            long artistId,
            Long genreId
    ) {
        String id = (me == null || me.getId() == null) ? null : me.getId().trim();
        if (id == null || id.isBlank()) {
            log.warn("Spotify related artists skipped: empty artist id");
            return List.of();
        }

        try {
            Artist[] related = api.getArtistsRelatedArtists(id).build().execute();
            if (related != null && related.length > 0) {
                log.info("Spotify related artists fetched: size={} spotifyArtistId={}", related.length, id);
                return toRelatedArtistResponses(related);
            }

            log.info("Spotify related artists empty -> fallback group/genre. spotifyArtistId={}", id);
            return fallbackRelatedFromDb(artistGroup, artistId, genreId);

        } catch (NotFoundException e) {
            log.warn("Spotify related artists NotFound (404) -> fallback group/genre. spotifyArtistId={}", id);
            return fallbackRelatedFromDb(artistGroup, artistId, genreId);

        } catch (Exception e) {
            log.error("Spotify related artists fetch error: spotifyArtistId={}", id, e);
            return fallbackRelatedFromDb(artistGroup, artistId, genreId);
        }
    }

    private List<RelatedArtistResponse> fallbackRelatedFromDb(String artistGroup, long artistId, Long genreId) {
        try {
            if (artistGroup != null && !artistGroup.isBlank()) {
                return artistRepository.findTop5ByArtistGroupAndIdNot(artistGroup, artistId).stream()
                        .map(a -> new RelatedArtistResponse(
                                a.getArtistName(),
                                null,
                                a.getSpotifyArtistId()
                        ))
                        .toList();
            }
            if (genreId != null) {
                return artistRepository.findTop5ByGenreIdAndIdNot(genreId, artistId).stream()
                        .map(a -> new RelatedArtistResponse(
                                a.getArtistName(),
                                null,
                                a.getSpotifyArtistId()
                        ))
                        .toList();
            }
        } catch (Exception e) {
            log.error("Fallback related from DB failed", e);
        }
        return List.of();
    }

    private String pickImageUrl(Image[] images) {
        if (images == null || images.length == 0) return null;
        return Arrays.stream(images)
                .filter(Objects::nonNull)
                .findFirst()
                .map(Image::getUrl)
                .orElse(null);
    }

    private List<AlbumResponse> toAlbumResponses(AlbumSimplified[] items, String artistId) {
        if (items == null) return List.of();
        return Stream.of(items)
                .filter(Objects::nonNull)
                // 앨범 타입 필터: album / single / ep
                .filter(a -> a.getAlbumType() == AlbumType.ALBUM
                        || a.getAlbumType() == AlbumType.SINGLE
                        || a.getAlbumType() == AlbumType.COMPILATION)
                // 해당 아티스트가 참여한 앨범만 필터링
                .filter(a -> {
                    if (a.getArtists() == null) return false;
                    return Arrays.stream(a.getArtists())
                            .anyMatch(ar -> ar != null && artistId != null && artistId.equals(ar.getId()));
                })
                .map(a -> new AlbumResponse(
                        a.getName(),
                        a.getReleaseDate(),
                        albumTypeToString(a.getAlbumType()),
                        pickImageUrl(a.getImages()),
                        a.getExternalUrls() != null ? a.getExternalUrls().get("spotify") : null
                ))
                .collect(toList());
    }

    private String albumTypeToString(AlbumType type) {
        if (type == null) return null;
        return type.getType();
    }

    private List<TopTrackResponse> toTopTrackResponses(Track[] tracks) {
        if (tracks == null) return List.of();
        return Stream.of(tracks)
                .filter(Objects::nonNull)
                .map(t -> new TopTrackResponse(
                        t.getName(),
                        t.getExternalUrls() != null ? t.getExternalUrls().get("spotify") : null
                ))
                .collect(toList());
    }

    private List<RelatedArtistResponse> toRelatedArtistResponses(Artist[] artists) {
        if (artists == null) return List.of();
        return Stream.of(artists)
                .filter(Objects::nonNull)
                .map(a -> new RelatedArtistResponse(
                        a.getName(),
                        pickImageUrl(a.getImages()),
                        a.getId()
                ))
                .collect(toList());
    }
}
