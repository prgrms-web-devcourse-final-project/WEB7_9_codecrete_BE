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
    
    // Rate Limiter: 모든 Spotify API 호출 전에 최소 간격 보장 (200ms)
    private final SimpleRateLimiter rateLimiter = new SimpleRateLimiter(200);

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
                        rateLimiter.acquire();
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

            // 2단계: 인기도 기준으로 상위 300명 선택
            final int maxSeedCount = 300;
            List<ArtistData> finalArtists = artistDataList;
            
            if (artistDataList.size() > maxSeedCount) {
                log.info("아티스트가 {}명으로 제한치({}명)를 초과합니다. 인기도 기준으로 상위 {}명만 선택합니다.", 
                        artistDataList.size(), maxSeedCount, maxSeedCount);
                
                finalArtists = artistDataList.stream()
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

            // 4단계: 각 아티스트를 DB에 upsert (spotifyArtistId 기준으로 있으면 업데이트, 없으면 생성)
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

            // 5단계: 모든 genres[]를 모아서 Set으로 중복 제거
            Set<String> allGenreNames = finalArtists.stream()
                    .flatMap(data -> data.genres.stream())
                    .filter(Objects::nonNull)
                    .filter(g -> !g.isBlank())
                    .collect(Collectors.toSet());

            if (allGenreNames.isEmpty()) {
                log.warn("수집된 장르가 없습니다.");
                return artistDataList.size();
            }

            // 6단계: DB에서 genre_name in (...)으로 기존 장르 한 번에 조회
            List<Genre> existingGenres = genreRepository.findByGenreNameIn(new ArrayList<>(allGenreNames));
            Set<String> existingGenreNames = existingGenres.stream()
                    .map(Genre::getGenreName)
                    .collect(Collectors.toSet());

            // 7단계: 없는 장르만 insert
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

            // 8단계: 각 아티스트별로 genres[]를 돌면서 artist_genre에 매핑 insert
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
     * Rate Limit 재시도 로직 (429 에러 처리)
     * 429면 Retry-After 헤더 확인 후 대기하고 재시도 (최대 3회)
     * 
     * @param supplier API 호출 함수
     * @param context 컨텍스트 정보 (로깅용)
     * @return API 호출 결과
     * @throws RuntimeException 재시도 모두 실패 시
     */
    private <T> T callWithRateLimitRetry(java.util.function.Supplier<T> supplier, String context) {
        final int maxRetry = 3;
        
        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            try {
                return supplier.get();
            } catch (Exception e) {
                // 원본 예외 확인 (래핑된 경우 cause 확인)
                Throwable originalException = e;
                Throwable current = e;
                
                // 예외 체인을 따라가며 TooManyRequestsException 찾기
                while (current != null) {
                    String currentClassName = current.getClass().getSimpleName();
                    if (currentClassName.contains("TooManyRequests")) {
                        originalException = current;
                        break;
                    }
                    current = current.getCause();
                }
                
                // 래핑된 경우 cause 확인
                if (originalException == e && e instanceof RuntimeException && e.getCause() != null) {
                    originalException = e.getCause();
                }
                
                String className = originalException.getClass().getSimpleName();
                String errorMsg = originalException.getMessage();
                String wrapperClassName = e.getClass().getSimpleName();
                String wrapperErrorMsg = e.getMessage();
                
                // 429 에러 확인 (원본 예외와 래핑된 예외 모두 확인)
                boolean is429 = className.contains("TooManyRequests") || 
                               (errorMsg != null && (errorMsg.contains("429") || errorMsg.contains("Too Many Requests"))) ||
                               wrapperClassName.contains("TooManyRequests") ||
                               (wrapperErrorMsg != null && (wrapperErrorMsg.contains("429") || wrapperErrorMsg.contains("Too Many Requests")));
                
                // 예외 체인 전체 확인
                if (!is429) {
                    current = e;
                    while (current != null) {
                        String currentClassName = current.getClass().getSimpleName();
                        String currentMsg = current.getMessage();
                        if (currentClassName.contains("TooManyRequests") || 
                            (currentMsg != null && (currentMsg.contains("429") || currentMsg.contains("Too Many Requests")))) {
                            is429 = true;
                            originalException = current;
                            break;
                        }
                        current = current.getCause();
                    }
                }
                
                // 디버깅 로그
                if (is429) {
                    log.warn("429 에러 감지: attempt={}/{}, className={}, wrapperClassName={}, errorMsg={}", 
                            attempt, maxRetry, className, wrapperClassName, errorMsg);
                } else {
                    log.debug("예외 발생 (429 아님): attempt={}/{}, className={}, wrapperClassName={}, errorMsg={}", 
                            attempt, maxRetry, className, wrapperClassName, errorMsg);
                }
                
                if (is429 && attempt < maxRetry) {
                    // Retry-After 헤더는 원본 예외에서 추출
                    Throwable headerException = originalException;
                    // Retry-After 헤더 추출 시도
                    long waitSec = 5; // 기본값 5초 (더 보수적으로)
                    final long minWaitSec = 3; // 최소 대기 시간 3초
                    final long bufferSec = 2; // 여유 시간 2초 추가
                    
                    try {
                        // Spotify SDK의 예외에서 Retry-After 헤더 추출 시도
                        // reflection을 통해 헤더 정보 확인 (원본 예외에서)
                        java.lang.reflect.Method getHeadersMethod = null;
                        try {
                            getHeadersMethod = headerException.getClass().getMethod("getResponseHeaders");
                        } catch (NoSuchMethodException ignored) {
                            // 메서드가 없으면 기본값 사용
                        }
                        
                        if (getHeadersMethod != null) {
                            try {
                                Object headers = getHeadersMethod.invoke(headerException);
                                if (headers != null && headers instanceof java.util.Map) {
                                    @SuppressWarnings("unchecked")
                                    java.util.Map<String, java.util.List<String>> headerMap = 
                                            (java.util.Map<String, java.util.List<String>>) headers;
                                    java.util.List<String> retryAfterList = headerMap.get("Retry-After");
                                    if (retryAfterList != null && !retryAfterList.isEmpty()) {
                                        try {
                                            long headerValue = Long.parseLong(retryAfterList.get(0));
                                            // 헤더 값 + 여유 시간, 최소값 보장
                                            waitSec = Math.max(minWaitSec, headerValue + bufferSec);
                                        } catch (NumberFormatException ignored) {
                                            // 파싱 실패 시 기본값 사용
                                        }
                                    }
                                }
                            } catch (Exception ignored) {
                                // 헤더 추출 실패 시 기본값 사용
                            }
                        }
                    } catch (Exception ignored) {
                        // reflection 실패 시 기본값 사용
                    }
                    
                    log.warn("{}: 429 Too Many Requests. attempt={}/{}. retry-after={}s (헤더값+{}초 여유)", 
                            context, attempt, maxRetry, waitSec, bufferSec);
                    
                    try {
                        Thread.sleep(waitSec * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(context + ": interrupted during retry wait", ie);
                    }
                    
                    // 재시도
                    continue;
                }
                
                // 429가 아니거나 재시도 횟수 초과 시 예외 재throw
                // IOException 등은 그대로 throw (호출부에서 처리)
                throw e;
            }
        }
        
        throw new RuntimeException(context + ": rate limit retry exhausted");
    }
    
    /**
     * 특정 market으로 앨범 발매일 조회 (1페이지로 최적화)
     * - limit=50, 1페이지만 조회 (페이지네이션 금지)
     * - include_groups=album,single (appears_on 제외)
     * - 본인 명의(primary) 발매만 포함: album.artists[0].id == targetArtistId
     * - 최신 발매일이 cutoffDate보다 오래되면 바로 제외
     * @param api Spotify API
     * @param spotifyId 아티스트 Spotify ID
     * @param market CountryCode (null이면 market 파라미터 없이 조회)
     * @param cutoffDate 활동 중 기준일 (이 날짜보다 최신이면 활동 중)
     * @return 최신 발매일, 없으면 null
     */
    private LocalDate getLatestReleaseDateWithMarket(SpotifyApi api, String spotifyId, CountryCode market, LocalDate cutoffDate) {
        try {
            // 1페이지로 최적화: limit=50, include_groups=album,single
            final int limit = 50;
            
            // Rate Limit 재시도 로직 적용
            Paging<AlbumSimplified> paging = callWithRateLimitRetry(() -> {
                try {
                    rateLimiter.acquire();
                    var requestBuilder = api.getArtistsAlbums(spotifyId)
                            .limit(limit)
                            .offset(0);
                    if (market != null) {
                        requestBuilder = requestBuilder.market(market);
                    }
                    return requestBuilder.build().execute();
                } catch (RuntimeException e) {
                    // RuntimeException (TooManyRequestsException 포함)은 그대로 throw
                    // TooManyRequestsException은 RuntimeException의 하위 클래스
                    throw e;
                } catch (Exception e) {
                    // IOException 등 checked exception을 RuntimeException으로 변환
                    // 단, TooManyRequestsException이 checked exception인 경우를 대비해 cause로 보존
                    RuntimeException wrapped = new RuntimeException("Exception during API call", e);
                    // 원본 예외 타입 정보를 메시지에 포함
                    wrapped.addSuppressed(e);
                    throw wrapped;
                }
            }, "getLatestReleaseDate spotifyId=" + spotifyId);
            
            if (paging == null || paging.getItems() == null || paging.getItems().length == 0) {
                log.debug("아티스트 앨범 없음: spotifyId={}, market={}", spotifyId, market);
                return null;
            }
            
            // 중복 제거를 위한 Set (albumId 기반)
            Set<String> seenAlbumIds = new HashSet<>();
            LocalDate newestDate = null;
            
            // 각 앨범을 필터링하고 최신 발매일 찾기
            for (AlbumSimplified album : paging.getItems()) {
                // 중복 제거: albumId 기반
                String albumId = album.getId();
                if (albumId == null || seenAlbumIds.contains(albumId)) {
                    continue;
                }
                seenAlbumIds.add(albumId);
                
                // 1. ALBUM, SINGLE만 포함 (appears_on은 이미 includeGroups로 제외됨)
                AlbumType albumType = album.getAlbumType();
                if (albumType != AlbumType.ALBUM && albumType != AlbumType.SINGLE) {
                    continue;
                }
                
                // 2. 본인 명의(primary) 발매만 포함: album.artists[0].id == targetArtistId
                if (!isPrimaryArtist(album, spotifyId)) {
                    continue;
                }
                
                // 3. 발매일 파싱 (release_date_precision 고려)
                String releaseDate = album.getReleaseDate();
                if (releaseDate == null || releaseDate.isBlank()) {
                    continue;
                }
                
                try {
                    LocalDate date = parseReleaseDateWithPrecision(releaseDate, album);
                    if (date != null) {
                        // 최신 발매일 업데이트
                        if (newestDate == null || date.isAfter(newestDate)) {
                            newestDate = date;
                        }
                        
                        // 최신 발매일이 cutoffDate보다 최근이면 바로 통과 (더 볼 필요 없음)
                        if (cutoffDate != null && newestDate.isAfter(cutoffDate)) {
                            log.debug("최신 발매일 조회 성공 (조기 종료): spotifyId={}, market={}, 최신 발매일={}, 기준일={}", 
                                    spotifyId, market, newestDate, cutoffDate);
                            return newestDate;
                        }
                    }
                } catch (Exception e) {
                    log.debug("발매일 파싱 실패: spotifyId={}, releaseDate={}, error={}", 
                            spotifyId, releaseDate, e.getMessage());
                }
            }
            
            if (newestDate == null) {
                log.debug("유효한 본인 명의 앨범/싱글 없음: spotifyId={}, market={}", spotifyId, market);
                return null;
            }
            
            LocalDate latestDate = newestDate;
            
            // 발매일 검증 및 로깅
            LocalDate now = LocalDate.now();
            if (latestDate.isAfter(now)) {
                log.error("잘못된 발매일 (미래): spotifyId={}, market={}, 최신 발매일={}", 
                        spotifyId, market, latestDate);
                return null;
            } else if (latestDate.isBefore(now.minusYears(10))) {
                log.warn("의심스러운 발매일 (10년 이상 오래됨): spotifyId={}, market={}, 최신 발매일={}", 
                        spotifyId, market, latestDate);
            } else if (latestDate.isBefore(now.minusYears(5))) {
                log.warn("최신 발매일이 5년 이상 오래됨: spotifyId={}, market={}, 최신 발매일={}", 
                        spotifyId, market, latestDate);
            } else {
                log.debug("최신 발매일 조회 성공: spotifyId={}, market={}, 최신 발매일={}", 
                        spotifyId, market, latestDate);
            }
            
            return latestDate;
            
        } catch (RuntimeException e) {
            // callWithRateLimitRetry에서 throw한 예외
            if (e.getMessage() != null && e.getMessage().contains("rate limit retry exhausted")) {
                log.warn("아티스트 최신 발매일 조회 실패 (재시도 모두 실패): spotifyId={}, market={}", 
                        spotifyId, market);
                return null;
            }
            // NotFoundException도 RuntimeException으로 처리
            String className = e.getClass().getSimpleName();
            if (className.contains("NotFound")) {
                log.debug("아티스트 앨범 정보 없음 (404): spotifyId={}, market={}", spotifyId, market);
                return null;
            }
            throw e;
        } catch (Exception e) {
            // IOException 등 기타 예외
            String className = e.getClass().getSimpleName();
            if (className.contains("NotFound")) {
                log.debug("아티스트 앨범 정보 없음 (404): spotifyId={}, market={}", spotifyId, market);
                return null;
            }
            log.warn("아티스트 최신 발매일 조회 실패: spotifyId={}, market={}, error={}", 
                    spotifyId, market, e.getMessage());
            return null;
        }
    }
    
    /**
     * 아티스트의 최신 앨범 발매일 조회 (한국 시장 기준)
     * include_groups=album,single만 조회하여 본인 명의 발매만 확인
     * 
     * @param api Spotify API
     * @param spotifyId 아티스트 Spotify ID
     * @return 최신 발매일, 없으면 null
     */
    private LocalDate getLatestReleaseDate(SpotifyApi api, String spotifyId) {
        return getLatestReleaseDateWithMarket(api, spotifyId, CountryCode.KR, null);
    }
    
    /**
     * 앨범이 본인 명의(primary) 발매인지 확인
     * 조건: album.artists[0].id == targetArtistId
     * 더 엄격하게: album.artists에 targetArtistId가 있어도 1번이 아니면 제외
     * 여러 명이면 콜라보일 수 있으니 제외
     * @param album 앨범
     * @param targetArtistId 타겟 아티스트 ID
     * @return 본인 명의 발매면 true
     */
    private boolean isPrimaryArtist(AlbumSimplified album, String targetArtistId) {
        if (album.getArtists() == null || album.getArtists().length == 0) {
            return false;
        }
        
        // 첫 번째 아티스트가 타겟 아티스트인지 확인
        var firstArtist = album.getArtists()[0];
        if (firstArtist == null || firstArtist.getId() == null) {
            return false;
        }
        
        // album.artists[0].id == targetArtistId
        boolean isPrimary = targetArtistId.equals(firstArtist.getId());
        
        // 더 엄격하게: 여러 명이면 콜라보일 수 있으니 제외
        if (isPrimary && album.getArtists().length > 1) {
            log.debug("콜라보 앨범 제외: albumId={}, artists={}", 
                    album.getId(), Arrays.stream(album.getArtists())
                            .map(a -> a != null ? a.getName() : null)
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining(", ")));
            return false;
        }
        
        return isPrimary;
    }
    
    /**
     * 앨범과 발매일을 함께 저장하는 임시 클래스
     */
    private static class AlbumWithDate {
        final AlbumSimplified album;
        final LocalDate releaseDate;
        
        AlbumWithDate(AlbumSimplified album, LocalDate releaseDate) {
            this.album = album;
            this.releaseDate = releaseDate;
        }
    }

    /**
     * 아티스트의 본인 명의 발매 수 조회 (ALBUM, SINGLE만 카운트, primary artist만)
     * @param api Spotify API
     * @param spotifyId 아티스트 Spotify ID
     * @return 발매 수
     */
    private int getReleaseCount(SpotifyApi api, String spotifyId) {
        try {
            // 최대 5페이지까지 조회하여 발매 수 카운트
            final int limit = 50;
            final int maxPages = 5;
            
            // 중복 제거를 위한 Set (albumId 기반)
            Set<String> seenAlbumIds = new HashSet<>();
            int count = 0;
            
            for (int page = 0; page < maxPages; page++) {
                int offset = page * limit;
                
                try {
                    rateLimiter.acquire();
                    Paging<AlbumSimplified> paging = api.getArtistsAlbums(spotifyId)
                            .market(CountryCode.KR)
                            .limit(limit)
                            .offset(offset)
                            .build()
                            .execute();
                    
                    if (paging == null || paging.getItems() == null || paging.getItems().length == 0) {
                        break;
                    }
                    
                    // 본인 명의(primary) ALBUM, SINGLE만 카운트
                    for (AlbumSimplified album : paging.getItems()) {
                        // 중복 제거
                        String albumId = album.getId();
                        if (albumId == null || seenAlbumIds.contains(albumId)) {
                            continue;
                        }
                        seenAlbumIds.add(albumId);
                        
                        // ALBUM, SINGLE만
                        AlbumType albumType = album.getAlbumType();
                        if (albumType != AlbumType.ALBUM && albumType != AlbumType.SINGLE) {
                            continue;
                        }
                        
                        // 본인 명의(primary) 발매만
                        if (isPrimaryArtist(album, spotifyId)) {
                            count++;
                        }
                    }
                    
                    // 더 이상 데이터가 없으면 중단
                    if (paging.getItems().length < limit) {
                        break;
                    }
                    
                    // Rate limit은 rateLimiter.acquire()에서 처리됨
                    
                } catch (Exception e) {
                    log.debug("앨범 페이지 조회 실패: spotifyId={}, page={}, error={}", 
                            spotifyId, page, e.getMessage());
                    if (page == 0) {
                        throw e;
                    }
                    break;
                }
            }
            
            return count;
            
        } catch (Exception e) {
            log.warn("아티스트 발매 수 조회 실패: spotifyId={}, error={}", spotifyId, e.getMessage());
            return 0;
        }
    }
    
    /**
     * 아티스트의 Followers 수 조회
     * @param api Spotify API
     * @param spotifyId 아티스트 Spotify ID
     * @return Followers 수, 없으면 null
     */
    private Integer getFollowers(SpotifyApi api, String spotifyId) {
        try {
            rateLimiter.acquire();
            se.michaelthelin.spotify.model_objects.specification.Artist artist = api.getArtist(spotifyId).build().execute();
            if (artist != null && artist.getFollowers() != null) {
                return artist.getFollowers().getTotal();
            }
            return null;
        } catch (Exception e) {
            log.debug("아티스트 Followers 조회 실패: spotifyId={}, error={}", spotifyId, e.getMessage());
            return null;
        }
    }
    
    /**
     * release_date와 release_date_precision을 고려하여 LocalDate로 파싱
     * release_date_precision: "day", "month", "year"
     * @param releaseDate 발매일 문자열
     * @param album 앨범 객체 (release_date_precision 확인용)
     * @return 파싱된 LocalDate
     */
    private LocalDate parseReleaseDateWithPrecision(String releaseDate, AlbumSimplified album) {
        if (releaseDate == null || releaseDate.isBlank()) {
            return null;
        }
        
        try {
            // release_date_precision 확인 (reflection 사용)
            String precision = "day"; // 기본값
            try {
                // AlbumSimplified의 releaseDatePrecision 필드 확인
                java.lang.reflect.Field precisionField = album.getClass().getDeclaredField("releaseDatePrecision");
                precisionField.setAccessible(true);
                Object precisionObj = precisionField.get(album);
                if (precisionObj != null) {
                    precision = precisionObj.toString().toLowerCase();
                }
            } catch (Exception e) {
                // 필드가 없거나 접근 불가능한 경우 길이로 추정
                if (releaseDate.length() == 4) {
                    precision = "year";
                } else if (releaseDate.length() == 7) {
                    precision = "month";
                } else if (releaseDate.length() == 10) {
                    precision = "day";
                }
            }
            
            // precision에 따라 파싱
            switch (precision) {
                case "day":
                    if (releaseDate.length() == 10) {
                        return LocalDate.parse(releaseDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    }
                    break;
                case "month":
                    if (releaseDate.length() == 7) {
                        // 월의 마지막 날로 설정 (더 정확한 비교를 위해)
                        LocalDate monthDate = LocalDate.parse(releaseDate + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        return monthDate.withDayOfMonth(monthDate.lengthOfMonth());
                    }
                    break;
                case "year":
                    if (releaseDate.length() == 4) {
                        // 연도의 마지막 날로 설정 (더 정확한 비교를 위해)
                        return LocalDate.parse(releaseDate + "-12-31", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    }
                    break;
            }
            
            // precision 정보가 없으면 길이로 추정 (기존 로직)
            if (releaseDate.length() == 10) {
                return LocalDate.parse(releaseDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } else if (releaseDate.length() == 7) {
                LocalDate monthDate = LocalDate.parse(releaseDate + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                return monthDate.withDayOfMonth(monthDate.lengthOfMonth());
            } else if (releaseDate.length() == 4) {
                return LocalDate.parse(releaseDate + "-12-31", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
            
        } catch (DateTimeParseException e) {
            log.debug("발매일 파싱 실패: releaseDate={}, error={}", releaseDate, e.getMessage());
        } catch (Exception e) {
            log.debug("발매일 파싱 중 예외: releaseDate={}, error={}", releaseDate, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * release_date 문자열을 LocalDate로 파싱 (기존 메서드, 호환성 유지)
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

            rateLimiter.acquire();
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
            rateLimiter.acquire();
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
            rateLimiter.acquire();
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
            rateLimiter.acquire();
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
    
    /**
     * 간단한 Rate Limiter (토큰 버킷 방식)
     * 모든 Spotify API 호출 전에 최소 간격을 보장하여 Rate Limit 위반 방지
     */
    private static class SimpleRateLimiter {
        private final long minIntervalMs;
        private long lastCallAt = 0;

        public SimpleRateLimiter(long minIntervalMs) {
            this.minIntervalMs = minIntervalMs;
        }

        public synchronized void acquire() {
            long now = System.currentTimeMillis();
            long wait = (lastCallAt + minIntervalMs) - now;
            if (wait > 0) {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            lastCallAt = System.currentTimeMillis();
        }
    }
}
