package com.back.web7_9_codecrete_be.domain.artists.service.spotify.application;

import com.back.web7_9_codecrete_be.domain.artists.dto.response.AlbumResponse;
import com.back.web7_9_codecrete_be.domain.artists.dto.response.SpotifyArtistDetailCache;
import com.back.web7_9_codecrete_be.domain.artists.dto.response.TopTrackResponse;
import com.back.web7_9_codecrete_be.domain.artists.service.spotify.rate_limit.SpotifyRateLimitHandler;
import com.back.web7_9_codecrete_be.global.spotify.SpotifyClient;
import com.neovisionaries.i18n.CountryCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.enums.AlbumType;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;


@Slf4j
@Service
@RequiredArgsConstructor
// Spotify API를 통한 아티스트 상세 정보 조회 서비스 : Spotify에서 아티스트 기본 정보, Top Tracks, 앨범 목록을 조회
public class SpotifyDetailService {

    private final SpotifyClient spotifyClient;
    private final SpotifyRateLimitHandler rateLimitHandler;

    private final Semaphore spotifyRateLimiter = new Semaphore(1);

    // Spotify API에서 아티스트 상세 정보 조회
    public SpotifyArtistDetailCache fetchDetailFromApi(String spotifyArtistId) {
        SpotifyApi api = spotifyClient.getAuthorizedApi();

        // 아티스트 기본 정보
        se.michaelthelin.spotify.model_objects.specification.Artist artist = rateLimitHandler.callWithRateLimitRetry(() -> {
            try {
                spotifyRateLimiter.acquire();
                return api.getArtist(spotifyArtistId).build().execute();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Exception during getArtist API call", e);
            }
        }, "getArtistDetail getArtist spotifyId=" + spotifyArtistId);

        // Top Tracks
        Track[] topTracks = safeGetTopTracks(api, spotifyArtistId);

        // 앨범 목록
        Paging<AlbumSimplified> albums = safeGetAlbums(api, spotifyArtistId);

        return new SpotifyArtistDetailCache(
                artist.getName(),
                pickImageUrl(artist.getImages()),
                artist.getPopularity(),
                toTopTrackResponses(topTracks),
                toAlbumResponses(albums != null ? albums.getItems() : null, spotifyArtistId),
                albums != null ? albums.getTotal() : 0
        );
    }

    private Track[] safeGetTopTracks(SpotifyApi api, String artistId) {
        try {
            return rateLimitHandler.callWithRateLimitRetry(() -> {
                try {
                    spotifyRateLimiter.acquire();
                    return api.getArtistsTopTracks(artistId, CountryCode.KR)
                            .build()
                            .execute();
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException("Exception during getArtistsTopTracks API call", e);
                }
            }, "safeGetTopTracks artistId=" + artistId);
        } catch (RuntimeException e) {
            return new Track[0];
        } catch (Exception e) {
            return new Track[0];
        }
    }

    private Paging<AlbumSimplified> safeGetAlbums(SpotifyApi api, String artistId) {
        try {
            return rateLimitHandler.callWithRateLimitRetry(() -> {
                try {
                    spotifyRateLimiter.acquire();
                    return api.getArtistsAlbums(artistId)
                            .market(CountryCode.KR)
                            .limit(20)
                            .build()
                            .execute();
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException("Exception during getArtistsAlbums API call", e);
                }
            }, "safeGetAlbums artistId=" + artistId);
        } catch (RuntimeException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public String pickImageUrl(Image[] images) {
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
                .filter(a -> a.getAlbumType() == AlbumType.ALBUM
                        || a.getAlbumType() == AlbumType.SINGLE
                        || a.getAlbumType() == AlbumType.COMPILATION)
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
}

