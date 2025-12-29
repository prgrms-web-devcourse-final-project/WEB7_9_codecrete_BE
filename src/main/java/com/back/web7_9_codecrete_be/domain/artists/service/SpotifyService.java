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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

            final int limit = 50;
            
            // 카테고리별 키워드 및 목표 수 정의
            Map<String, CategoryConfig> categoryConfigs = Map.of(
                    "K-POP/대중", new CategoryConfig(List.of(
                            "k-pop", "k-pop girl group", "k-pop boy group", 
                            "k-pop solo", "k-pop ost"
                    ), 200, true), // 한국 음악 필터링 적용
                    "국내 힙합/R&B", new CategoryConfig(List.of(
                            "k-rap", "korean hip hop", "k-r&b", "korean r&b"
                    ), 60, true), // 한국 음악 필터링 적용
                    "국내 인디/밴드", new CategoryConfig(List.of(
                            "k-indie", "korean indie", "korean indie rock", 
                            "korean rock", "korean band"
                    ), 70, true), // 한국 음악 필터링 적용
                    "발라드/OST", new CategoryConfig(List.of(
                            "k-ballad", "korean ballad", "k-ost", "korean ost"
                    ), 40, true), // 한국 음악 필터링 적용
                    "글로벌", new CategoryConfig(List.of(
                            "pop", "hip hop", "r&b", "rock", "alternative rock", 
                            "edm", "indie pop", "j-pop"
                    ), 30, false) // 한국 음악 필터링 미적용
            );
            
            // 1단계: 카테고리별로 아티스트 수집
            List<ArtistData> artistDataList = new ArrayList<>();
            Set<String> collectedSpotifyIds = new HashSet<>(); // 전체 중복 방지를 위한 Set
            
            for (Map.Entry<String, CategoryConfig> entry : categoryConfigs.entrySet()) {
                String categoryName = entry.getKey();
                CategoryConfig config = entry.getValue();
                int targetCount = config.targetCount;
                int collected = 0;
                
                log.info("카테고리 '{}' 수집 시작 (목표: {}명)", categoryName, targetCount);
                
                for (String query : config.keywords) {
                    if (collected >= targetCount) break;
                    
                    int offset = 0;
                    
                    while (collected < targetCount) {
                        Paging<se.michaelthelin.spotify.model_objects.specification.Artist> paging = api.searchArtists(query)
                                        .limit(limit)
                                        .offset(offset)
                                        .build()
                                        .execute();

                        var items = paging.getItems();
                        if (items == null || items.length == 0) break;

                        for (var spotifyArtist : items) {
                            if (collected >= targetCount) break;

                            String spotifyId = spotifyArtist.getId();
                            String name = spotifyArtist.getName();
                            String[] genres = spotifyArtist.getGenres();

                            if (spotifyId == null || name == null || name.isBlank()) continue;
                            if (collectedSpotifyIds.contains(spotifyId)) continue; // 전체 중복 체크
                            
                            // 한국 음악 필터링 (글로벌은 제외)
                            if (config.requireKorean && !isLikelyKoreanMusic(spotifyArtist)) continue;

                            String artistTypeStr = inferArtistType(spotifyArtist);
                            ArtistType artistType = ArtistType.valueOf(artistTypeStr);
                            String imageUrl = pickImageUrl(spotifyArtist.getImages());
                            Integer popularity = spotifyArtist.getPopularity(); // Spotify 인기도

                            // genres 배열을 List로 변환 (null 제거)
                            List<String> genreList = genres != null 
                                    ? Arrays.stream(genres).filter(Objects::nonNull).filter(g -> !g.isBlank()).collect(toList())
                                    : List.of();

                            artistDataList.add(new ArtistData(spotifyId, name.trim(), artistType, imageUrl, genreList, popularity));
                            collectedSpotifyIds.add(spotifyId); // 수집된 spotifyId 추가
                            collected++;
                        }

                        offset += limit;
                        if (offset >= paging.getTotal()) break;

                        Thread.sleep(200);
                    }
                }
                
                log.info("카테고리 '{}' 수집 완료: {}명 (목표: {}명)", categoryName, collected, targetCount);
            }

            if (artistDataList.isEmpty()) {
                throw new BusinessException(ArtistErrorCode.ARTIST_SEED_FAILED);
            }

            log.info("아티스트 후보 수집 완료: {}명", artistDataList.size());

            // 2단계: 활동 중인 아티스트만 필터링 (최신 발매일 기준)
            final int activeMonths = 24; // 최근 24개월(2년) 이내 발매한 아티스트만 활동 중으로 간주
            List<ArtistData> activeArtists = filterActiveArtists(api, artistDataList, activeMonths);
            
            if (activeArtists.isEmpty()) {
                log.warn("활동 중인 아티스트가 없습니다.");
                throw new BusinessException(ArtistErrorCode.ARTIST_SEED_FAILED);
            }
            
            log.info("활동 중인 아티스트 필터링 완료: {}명 (후보 {}명 중)", activeArtists.size(), artistDataList.size());

            // 2-1단계: 활동 중인 아티스트가 300명을 넘으면 인기도 기준으로 상위 300명만 선택
            final int maxSeedCount = 300;
            List<ArtistData> finalArtists = activeArtists;
            if (activeArtists.size() > maxSeedCount) {
                log.info("활동 중인 아티스트가 {}명으로 제한치({}명)를 초과합니다. 인기도 기준으로 상위 {}명만 선택합니다.", 
                        activeArtists.size(), maxSeedCount, maxSeedCount);
                
                finalArtists = activeArtists.stream()
                        .sorted((a1, a2) -> {
                            // popularity가 null인 경우 가장 낮은 우선순위
                            Integer p1 = a1.popularity != null ? a1.popularity : -1;
                            Integer p2 = a2.popularity != null ? a2.popularity : -1;
                            return Integer.compare(p2, p1); // 내림차순 정렬 (높은 인기도가 먼저)
                        })
                        .limit(maxSeedCount)
                        .collect(toList());
                
                log.info("인기도 기준 정렬 완료: 상위 {}명 선택", finalArtists.size());
            }

            // 3단계: 각 아티스트를 DB에 upsert (spotifyArtistId 기준으로 있으면 업데이트, 없으면 생성)
            Map<String, Artist> artistMap = new HashMap<>(); // spotifyId -> Artist 매핑
            for (ArtistData data : finalArtists) {
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

            // 4단계: 모든 genres[]를 모아서 Set으로 중복 제거
            Set<String> allGenreNames = finalArtists.stream()
                    .flatMap(data -> data.genres.stream())
                    .filter(Objects::nonNull)
                    .filter(g -> !g.isBlank())
                    .collect(Collectors.toSet());

            if (allGenreNames.isEmpty()) {
                log.warn("수집된 장르가 없습니다.");
                return artistDataList.size();
            }

            // 5단계: DB에서 genre_name in (...)으로 기존 장르 한 번에 조회
            List<Genre> existingGenres = genreRepository.findByGenreNameIn(new ArrayList<>(allGenreNames));
            Set<String> existingGenreNames = existingGenres.stream()
                    .map(Genre::getGenreName)
                    .collect(Collectors.toSet());

            // 6단계: 없는 장르만 insert
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

            // 7단계: 각 아티스트별로 genres[]를 돌면서 artist_genre에 매핑 insert
            int totalMappings = 0;
            for (ArtistData data : finalArtists) {
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
                    finalArtists.size(), genreMap.size(), totalMappings);

            return finalArtists.size();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("아티스트 시드 실패", e);
            throw new BusinessException(ArtistErrorCode.SPOTIFY_API_ERROR);
        }
    }

    // 카테고리 설정 클래스
    private static class CategoryConfig {
        final List<String> keywords;
        final int targetCount;
        final boolean requireKorean; // 한국 음악 필터링 필요 여부

        CategoryConfig(List<String> keywords, int targetCount, boolean requireKorean) {
            this.keywords = keywords;
            this.targetCount = targetCount;
            this.requireKorean = requireKorean;
        }
    }

    // 아티스트 데이터 임시 저장용 클래스
    private static class ArtistData {
        final String spotifyId;
        final String name;
        final ArtistType artistType;
        final String imageUrl;
        final List<String> genres;
        final Integer popularity; // Spotify 인기도 (0-100)

        ArtistData(String spotifyId, String name, ArtistType artistType, String imageUrl, List<String> genres, Integer popularity) {
            this.spotifyId = spotifyId;
            this.name = name;
            this.artistType = artistType;
            this.imageUrl = imageUrl;
            this.genres = genres;
            this.popularity = popularity;
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

    /**
     * 활동 중인 아티스트만 필터링 (최신 발매일 기준)
     * @param api Spotify API
     * @param artistDataList 아티스트 후보 리스트
     * @param activeMonths 최근 N개월 이내 발매한 아티스트만 활동 중으로 간주
     * @return 활동 중인 아티스트 리스트
     */
    private List<ArtistData> filterActiveArtists(
            SpotifyApi api,
            List<ArtistData> artistDataList,
            int activeMonths
    ) {
        List<ArtistData> activeArtists = new ArrayList<>();
        LocalDate cutoffDate = LocalDate.now().minusMonths(activeMonths);
        
        log.info("활동 중 아티스트 필터링 시작: 후보 {}명, 기준일: {} (최근 {}개월)", 
                artistDataList.size(), cutoffDate, activeMonths);
        
        int checked = 0;
        int active = 0;
        int inactive = 0;
        
        for (ArtistData data : artistDataList) {
            checked++;
            try {
                LocalDate latestReleaseDate = getLatestReleaseDate(api, data.spotifyId);
                
                // 조건 수정: latestReleaseDate가 null이 아니고, cutoffDate 이후 또는 같으면 활동 중
                boolean isActive = latestReleaseDate != null && 
                        (latestReleaseDate.isAfter(cutoffDate) || latestReleaseDate.isEqual(cutoffDate));
                
                if (isActive) {
                    activeArtists.add(data);
                    active++;
                    log.debug("활동 중 아티스트: {} (최신 발매일: {}, 기준일: {})", 
                            data.name, latestReleaseDate, cutoffDate);
                } else {
                    inactive++;
                    if (latestReleaseDate != null) {
                        log.debug("비활동 아티스트 제외: {} (최신 발매일: {}, 기준일: {})", 
                                data.name, latestReleaseDate, cutoffDate);
                    } else {
                        log.debug("비활동 아티스트 제외: {} (앨범 정보 없음)", data.name);
                    }
                }
                
                // 진행 상황 로깅 (50명마다)
                if (checked % 50 == 0) {
                    log.info("활동 중 아티스트 필터링 진행: {}/{} (활동: {}, 비활동: {})", 
                            checked, artistDataList.size(), active, inactive);
                }
                
                Thread.sleep(100); // Rate limit 방지
                
            } catch (Exception e) {
                log.warn("아티스트 최신 발매일 조회 실패: spotifyId={}, name={}, error={}", 
                        data.spotifyId, data.name, e.getMessage());
                // 조회 실패 시 비활동으로 간주
                inactive++;
            }
        }
        
        log.info("활동 중 아티스트 필터링 완료: 전체 {}명 중 활동 {}명, 비활동 {}명", 
                checked, active, inactive);
        
        return activeArtists;
    }

    /**
     * 아티스트의 최신 앨범 발매일 조회
     * GET /v1/artists/{id}/albums?include_groups=album,single&market=KR&limit=20
     * @param api Spotify API
     * @param spotifyId 아티스트 Spotify ID
     * @return 최신 발매일 (yyyy-MM-dd 형식 파싱), 없으면 null
     */
    private LocalDate getLatestReleaseDate(SpotifyApi api, String spotifyId) {
        try {
            // GET /v1/artists/{id}/albums?market=KR&limit=50
            // limit을 50으로 늘려서 더 많은 앨범을 확인
            // include_groups는 API 레벨에서 지원하지 않으므로, 조회 후 필터링
            Paging<AlbumSimplified> albums = api.getArtistsAlbums(spotifyId)
                    .market(CountryCode.KR)
                    .limit(50) // 20에서 50으로 증가
                    .build()
                    .execute();
            
            if (albums == null || albums.getItems() == null || albums.getItems().length == 0) {
                log.debug("아티스트 앨범 없음: spotifyId={}", spotifyId);
                return null;
            }
            
            LocalDate latestDate = null;
            int checkedCount = 0;
            
            for (AlbumSimplified album : albums.getItems()) {
                // album, single만 포함 (ep, compilation 제외)
                AlbumType albumType = album.getAlbumType();
                if (albumType != AlbumType.ALBUM && albumType != AlbumType.SINGLE) {
                    continue;
                }
                
                String releaseDate = album.getReleaseDate();
                if (releaseDate == null || releaseDate.isBlank()) {
                    continue;
                }
                
                checkedCount++;
                try {
                    // release_date 형식: "yyyy-MM-dd" 또는 "yyyy" 또는 "yyyy-MM"
                    LocalDate date = parseReleaseDate(releaseDate);
                    if (date != null) {
                        if (latestDate == null || date.isAfter(latestDate)) {
                            latestDate = date;
                        }
                    }
                } catch (Exception e) {
                    log.debug("발매일 파싱 실패: spotifyId={}, releaseDate={}, error={}", 
                            spotifyId, releaseDate, e.getMessage());
                }
            }
            
            if (latestDate == null && checkedCount > 0) {
                log.debug("앨범은 있지만 유효한 발매일 없음: spotifyId={}, 확인한 앨범 수={}", 
                        spotifyId, checkedCount);
            }
            
            return latestDate;
            
        } catch (NotFoundException e) {
            log.debug("아티스트 앨범 정보 없음 (404): spotifyId={}", spotifyId);
            return null;
        } catch (Exception e) {
            log.warn("아티스트 최신 발매일 조회 실패: spotifyId={}, error={}", spotifyId, e.getMessage());
            return null;
        }
    }

    /**
     * release_date 문자열을 LocalDate로 파싱
     * 지원 형식: "yyyy-MM-dd", "yyyy-MM", "yyyy"
     */
    private LocalDate parseReleaseDate(String releaseDate) {
        if (releaseDate == null || releaseDate.isBlank()) {
            return null;
        }
        
        try {
            // "yyyy-MM-dd" 형식
            if (releaseDate.length() == 10) {
                return LocalDate.parse(releaseDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
            // "yyyy-MM" 형식
            else if (releaseDate.length() == 7) {
                return LocalDate.parse(releaseDate + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
            // "yyyy" 형식
            else if (releaseDate.length() == 4) {
                return LocalDate.parse(releaseDate + "-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
        } catch (DateTimeParseException e) {
            log.debug("발매일 파싱 실패: releaseDate={}", releaseDate);
        }
        
        return null;
    }


    @Transactional(readOnly = true)
    public ArtistDetailResponse getArtistDetail(
            String spotifyArtistId,
            String artistGroup,
            ArtistType artistType,
            long likeCount,
            long artistId,
            Long genreId
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
                    (long) artistId,
                    artist.getName(),
                    nameKo,
                    artistGroup,
                    artistType,
                    pickImageUrl(artist.getImages()),
                    likeCount,
                    albums != null ? albums.getTotal() : 0,
                    artist.getPopularity(),
                    "",
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
