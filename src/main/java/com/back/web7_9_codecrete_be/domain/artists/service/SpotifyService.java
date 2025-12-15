package com.back.web7_9_codecrete_be.domain.artists.service;

import com.back.web7_9_codecrete_be.domain.artists.entity.Artist;
import com.back.web7_9_codecrete_be.domain.artists.entity.Genre;
import com.back.web7_9_codecrete_be.domain.artists.repository.ArtistRepository;
import com.back.web7_9_codecrete_be.domain.artists.repository.GenreRepository;
import com.back.web7_9_codecrete_be.global.error.code.ArtistErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import com.back.web7_9_codecrete_be.global.spotify.SpotifyClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Paging;

import java.util.List;

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
                    Paging<se.michaelthelin.spotify.model_objects.specification.Artist> paging =
                            api.searchArtists(q)
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

                        // ✅ seed 단계에서는 Wikidata 호출 금지 (속도/실패 리스크)
                        Artist artist = new Artist(
                                spotifyId,
                                name.trim(),
                                null,        // artistGroup
                                artistType,
                                genre
                        );

                        artistRepository.save(artist);
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

    private boolean isLikelyKoreanMusic(se.michaelthelin.spotify.model_objects.specification.Artist a) {
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
        // 한글 이름 보조 필터
        String name = a.getName();
        return name != null && name.matches(".*[가-힣].*");
    }

    private String pickMainGenreName(se.michaelthelin.spotify.model_objects.specification.Artist a) {
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

    private String inferArtistType(se.michaelthelin.spotify.model_objects.specification.Artist a) {
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
}
