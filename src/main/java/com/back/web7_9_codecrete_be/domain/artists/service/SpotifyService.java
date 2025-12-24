package com.back.web7_9_codecrete_be.domain.artists.service;

import com.back.web7_9_codecrete_be.domain.artists.dto.response.AlbumResponse;
import com.back.web7_9_codecrete_be.domain.artists.dto.response.ArtistDetailResponse;
import com.back.web7_9_codecrete_be.domain.artists.dto.response.RelatedArtistResponse;
import com.back.web7_9_codecrete_be.domain.artists.dto.response.TopTrackResponse;
import com.back.web7_9_codecrete_be.domain.artists.entity.Artist;
import com.back.web7_9_codecrete_be.domain.artists.entity.ArtistGenre;
import com.back.web7_9_codecrete_be.domain.artists.entity.ArtistType;
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
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.*;
import java.util.stream.Collectors;
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
            
            // 1단계: Spotify에서 아티스트 300명 조회 (spotifyArtistId, 이름, genres[] 포함)
            List<ArtistData> artistDataList = new ArrayList<>();
            List<String> queries = List.of(
                    "k-pop", "korean pop",
                    "BTS", "BLACKPINK", "NewJeans", "LE SSERAFIM", "aespa", "IVE", "NCT",
                    "Stray Kids", "TWICE", "Red Velvet", "IU", "태연",
                    "korean hip hop", "korean r&b", "korean ballad", "korean ost",
                    "korean indie", "korean rock", "trot"
            );

            for (String q : queries) {
                if (artistDataList.size() >= targetCount) break;

                int offset = 0;

                while (artistDataList.size() < targetCount) {
                    Paging<se.michaelthelin.spotify.model_objects.specification.Artist> paging = api.searchArtists(q)
                                    .limit(limit)
                                    .offset(offset)
                                    .build()
                                    .execute();

                    var items = paging.getItems();
                    if (items == null || items.length == 0) break;

                    for (var spotifyArtist : items) {
                        if (artistDataList.size() >= targetCount) break;

                        String spotifyId = spotifyArtist.getId();
                        String name = spotifyArtist.getName();
                        String[] genres = spotifyArtist.getGenres();

                        if (spotifyId == null || name == null || name.isBlank()) continue;
                        if (!isLikelyKoreanMusic(spotifyArtist)) continue;

                        String artistTypeStr = inferArtistType(spotifyArtist);
                        ArtistType artistType = ArtistType.valueOf(artistTypeStr);
                        String imageUrl = pickImageUrl(spotifyArtist.getImages());

                        // genres 배열을 List로 변환 (null 제거)
                        List<String> genreList = genres != null 
                                ? Arrays.stream(genres).filter(Objects::nonNull).filter(g -> !g.isBlank()).collect(toList())
                                : List.of();

                        artistDataList.add(new ArtistData(spotifyId, name.trim(), artistType, imageUrl, genreList));
                    }

                    offset += limit;
                    if (offset >= paging.getTotal()) break;

                    Thread.sleep(200);
                }
            }

            if (artistDataList.isEmpty()) {
                throw new BusinessException(ArtistErrorCode.ARTIST_SEED_FAILED);
            }

            // 2단계: 각 아티스트를 DB에 upsert (spotifyArtistId 기준으로 있으면 업데이트, 없으면 생성)
            Map<String, Artist> artistMap = new HashMap<>(); // spotifyId -> Artist 매핑
            for (ArtistData data : artistDataList) {
                Optional<Artist> existingArtistOpt = artistRepository.findBySpotifyArtistId(data.spotifyId);
                
                Artist artist;
                if (existingArtistOpt.isPresent()) {
                    // 업데이트
                    artist = existingArtistOpt.get();
                    artist.setArtistName(data.name);
                    artist.setArtistType(data.artistType);
                    artist.setImageUrl(data.imageUrl);
                    // 기존 장르 관계 제거
                    artist.getArtistGenres().clear();
                } else {
                    // 생성
                    artist = new Artist(data.spotifyId, data.name, null, data.artistType);
                    artist.setImageUrl(data.imageUrl);
                }
                
                artistRepository.save(artist);
                artistMap.put(data.spotifyId, artist);
            }

            // 3단계: 모든 genres[]를 모아서 Set으로 중복 제거
            Set<String> allGenreNames = artistDataList.stream()
                    .flatMap(data -> data.genres.stream())
                    .filter(Objects::nonNull)
                    .filter(g -> !g.isBlank())
                    .collect(Collectors.toSet());

            if (allGenreNames.isEmpty()) {
                log.warn("수집된 장르가 없습니다.");
                return artistDataList.size();
            }

            // 4단계: DB에서 genre_name in (...)으로 기존 장르 한 번에 조회
            List<Genre> existingGenres = genreRepository.findByGenreNameIn(new ArrayList<>(allGenreNames));
            Set<String> existingGenreNames = existingGenres.stream()
                    .map(Genre::getGenreName)
                    .collect(Collectors.toSet());

            // 5단계: 없는 장르만 insert
            List<String> newGenreNames = allGenreNames.stream()
                    .filter(name -> !existingGenreNames.contains(name))
                    .collect(toList());

            if (!newGenreNames.isEmpty()) {
                List<Genre> newGenres = newGenreNames.stream()
                        .map(Genre::new)
                        .collect(toList());
                genreRepository.saveAll(newGenres);
                existingGenres.addAll(newGenres);
                log.info("새로운 장르 {}개 생성: {}", newGenres.size(), newGenreNames);
            }

            // Genre Map 생성 (genreName -> Genre)
            Map<String, Genre> genreMap = existingGenres.stream()
                    .collect(Collectors.toMap(Genre::getGenreName, g -> g, (g1, g2) -> g1));

            // 6단계: 각 아티스트별로 genres[]를 돌면서 artist_genre에 매핑 insert
            int totalMappings = 0;
            for (ArtistData data : artistDataList) {
                Artist artist = artistMap.get(data.spotifyId);
                if (artist == null) {
                    log.warn("아티스트를 찾을 수 없음: spotifyId={}", data.spotifyId);
                    continue;
                }

                for (String genreName : data.genres) {
                    if (genreName == null || genreName.isBlank()) continue;
                    
                    Genre genre = genreMap.get(genreName);
                    if (genre == null) {
                        log.warn("장르를 찾을 수 없음: genreName={}, spotifyId={}", genreName, data.spotifyId);
                        continue;
                    }

                    // 중복 방지는 UNIQUE(artist_id, genre_id)로 해결
                    // 이미 존재하는지 확인
                    boolean alreadyExists = artist.getArtistGenres().stream()
                            .anyMatch(ag -> ag.getGenre().getId() == genre.getId());
                    
                    if (!alreadyExists) {
                        ArtistGenre artistGenre = new ArtistGenre(artist, genre);
                        artist.getArtistGenres().add(artistGenre);
                        totalMappings++;
                    }
                }
                
                artistRepository.save(artist);
            }

            log.info("아티스트 시드 완료: 아티스트 {}명, 장르 {}개, 매핑 {}개", 
                    artistDataList.size(), genreMap.size(), totalMappings);

            return artistDataList.size();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("아티스트 시드 실패", e);
            throw new BusinessException(ArtistErrorCode.SPOTIFY_API_ERROR);
        }
    }

    // 아티스트 데이터 임시 저장용 클래스
    private static class ArtistData {
        final String spotifyId;
        final String name;
        final ArtistType artistType;
        final String imageUrl;
        final List<String> genres;

        ArtistData(String spotifyId, String name, ArtistType artistType, String imageUrl, List<String> genres) {
            this.spotifyId = spotifyId;
            this.name = name;
            this.artistType = artistType;
            this.imageUrl = imageUrl;
            this.genres = genres;
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


    @Transactional(readOnly = true)
    public ArtistDetailResponse getArtistDetail(
            String spotifyArtistId,
            String artistGroup,
            ArtistType artistType,
            long likeCount,
            long artistId,
            Long genreId,
            boolean isLiked
    ) {
        try {
            SpotifyApi api = spotifyClient.getAuthorizedApi();

            se.michaelthelin.spotify.model_objects.specification.Artist artist = api.getArtist(spotifyArtistId).build().execute(); // 메인 정보는 실패 시 예외 발생

            // DB에서 아티스트 정보 조회하여 nameKo 가져오기
            Artist dbArtist = artistRepository.findById(artistId)
                    .orElse(null);
            String nameKo = dbArtist != null ? dbArtist.getNameKo() : null;

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
                    artistId,
                    artist.getName(),
                    nameKo,
                    artistGroup,
                    artistType,
                    pickImageUrl(artist.getImages()),
                    likeCount,
                    albums != null ? albums.getTotal() : 0,
                    artist.getPopularity(), // 별점으로 수정
                    "", // 설명
                    toAlbumResponses(albums != null ? albums.getItems() : null, spotifyArtistId),
                    toTopTrackResponses(topTracks),
                    relatedResponses,
                    isLiked
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

    private List<RelatedArtistResponse> safeGetRelated(
            SpotifyApi api,
            se.michaelthelin.spotify.model_objects.specification.Artist me,
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
            se.michaelthelin.spotify.model_objects.specification.Artist[] related = api.getArtistsRelatedArtists(id).build().execute();
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
                                a.getNameKo(),
                                a.getImageUrl(),
                                a.getSpotifyArtistId()
                        ))
                        .toList();
            }
            if (genreId != null) {
                return artistRepository.findTop5ByGenreIdAndIdNot(genreId, artistId, 
                        org.springframework.data.domain.PageRequest.of(0, 5)).stream()
                        .map(a -> new RelatedArtistResponse(
                                a.getArtistName(),
                                a.getNameKo(),
                                a.getImageUrl(),
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

    private List<RelatedArtistResponse> toRelatedArtistResponses(se.michaelthelin.spotify.model_objects.specification.Artist[] artists) {
        if (artists == null) return List.of();
        return Stream.of(artists)
                .filter(Objects::nonNull)
                .map(a -> {
                    // DB에서 아티스트 정보 조회하여 nameKo 가져오기
                    String nameKo = null;
                    Optional<Artist> dbArtist = artistRepository.findBySpotifyArtistId(a.getId());
                    if (dbArtist.isPresent()) {
                        nameKo = dbArtist.get().getNameKo();
                    }
                    return new RelatedArtistResponse(
                            a.getName(),
                            nameKo,
                            pickImageUrl(a.getImages()),
                            a.getId()
                    );
                })
                .collect(toList());
    }
}
