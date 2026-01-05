package com.back.web7_9_codecrete_be.domain.artists.service.spotifyService;

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
import com.back.web7_9_codecrete_be.global.musicbrainz.MusicBrainzClient;
import com.back.web7_9_codecrete_be.global.spotify.SpotifyClient;
import com.back.web7_9_codecrete_be.global.wikidata.WikidataClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.neovisionaries.i18n.CountryCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.enums.AlbumType;
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
    private final MusicBrainzClient musicBrainzClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final WikidataClient wikidataClient;
    private final SpotifyRateLimitHandler rateLimitHandler;
    
    // Rate Limiter 설정
    private static final long SPOTIFY_RATE_LIMIT_INTERVAL_MS = 500; // 초당 2회
    private static final long MUSICBRAINZ_RATE_LIMIT_INTERVAL_MS = 1000; // 초당 1회
    
    // 아티스트 시드 설정 (FastSeed 모드)
    private static final int SEARCH_LIMIT = 50;
    private static final int MAX_ARTISTS_PER_CATEGORY = 50; // 카테고리당 최대 수집 수 (FastSeed)
    private static final int MAX_PAGES_PER_KEYWORD = 1; // 키워드당 최대 페이지 수 (FastSeed: 1페이지만)
    private static final int MAX_SEED_COUNT = 300; // 최종 저장할 아티스트 수
    private static final int API_CALL_DELAY_MS = 200; // API 호출 간 대기 시간
    
    // 국내/해외 분류 및 쿼터 설정
    private static final int TARGET_KOREAN_COUNT = 250; // 국내 목표 수 (240~250명)
    private static final int MAX_GLOBAL_COUNT = 50; // 해외 최대 수 (15~18%, 쿼터제)
    private static final int GLOBAL_QUOTA_PERCENT = 18; // 해외 쿼터 비율 (%)
    
    // 필터링 기준 (카테고리별)
    // (A) 아이돌(국내 K-POP)
    private static final int IDOL_MIN_POPULARITY = 25;
    private static final int IDOL_MIN_FOLLOWERS = 5000;
    private static final int IDOL_UNCONDITIONAL_POPULARITY = 65;
    
    // (B) 힙합/R&B
    private static final int HIPHOP_MIN_POPULARITY = 20;
    private static final int HIPHOP_MIN_FOLLOWERS = 3000;
    private static final int HIPHOP_UNCONDITIONAL_POPULARITY = 55;
    
    // (C) 인디/밴드
    private static final int BAND_MIN_POPULARITY = 15;
    private static final int BAND_MIN_FOLLOWERS = 1500;
    
    // (D) 발라드/OST
    private static final int BALLAD_MIN_POPULARITY = 20;
    private static final int BALLAD_MIN_FOLLOWERS = 2000;
    
    // (E) 글로벌
    private static final int GLOBAL_MIN_POPULARITY = 15;
    private static final int GLOBAL_MIN_FOLLOWERS = 20000;
    
    // 대중성 점수 기준
    private static final int UNCONDITIONAL_POPULARITY = 65;
    private static final int UNCONDITIONAL_FOLLOWERS = 1_000_000;
    private static final int KOREAN_LEGACY_POPULARITY = 40;
    private static final int KOREAN_LEGACY_FOLLOWERS = 300_000;
    private static final int GLOBAL_LEGACY_POPULARITY = 30;
    private static final int GLOBAL_LEGACY_FOLLOWERS = 200_000;
    private static final int MB_FAILED_MIN_POPULARITY = 20;
    private static final int MB_FAILED_MIN_FOLLOWERS = 5000;
    
    // Rate Limiter 인스턴스
    private final SimpleRateLimiter spotifyRateLimiter = new SimpleRateLimiter(SPOTIFY_RATE_LIMIT_INTERVAL_MS);
    private final SimpleRateLimiter musicBrainzRateLimiter = new SimpleRateLimiter(MUSICBRAINZ_RATE_LIMIT_INTERVAL_MS);
    
    private static final List<String> KOREAN_GENRE_HINTS = List.of(
            "k-pop", "korean", "trot",
            "k-hip hop", "k-rap", "k-ballad", "k-r&b", "k-indie", "k-rock",
            "korean hip hop", "korean r&b", "korean ballad", "korean ost",
            "korean indie", "korean rock"
    );
    
    private static final List<String> SURVIVAL_PROGRAM_KEYWORDS = List.of(
            "produce 101", "produce 48", "produce x 101", "produce x",
            "girls planet", "boys planet", "girls planet 999", "boys planet 999",
            "queendom puzzle", "queendom",
            "universe ticket", "universe",
            "r u next", "runext",
            "dream academy", "dreamacademy",
            "i-land", "iland",
            "wild idol", "wildidol",
            "my teenage girl", "teenage girl",
            "peak time", "peaktime",
            "show me the money", "showmethemoney",
            "mixnine", "mix nine",
            "the unit", "theunit",
            "under nineteen", "under19",
            "idol school", "idolschool",
            "k-pop star", "kpop star",
            "superstar k", "superstark"
    );
    
    private static final List<String> COMPILATION_KEYWORDS = List.of(
            "가수들", "싱어들", "아티스트들", "밴드들", "그룹들",
            "singers", "artists", "bands", "groups",
            "모음", "베스트", "best of", "collection", "compilation",
            "발라드 싱어", "발라드 가수", "발라드 아티스트",
            "ballad singer", "ballad singers", "ballad artists",
            "힙합 아티스트", "인디 밴드", "록 밴드",
            "hip hop artists", "indie bands", "rock bands"
    );

    @Transactional
    public int seedKoreanArtists300() {
        try {
            SpotifyApi api = spotifyClient.getAuthorizedApi();
            Map<String, CategoryConfig> categoryConfigs = createCategoryConfigs();
            
            List<ArtistData> artistDataList = collectArtistsRoundRobin(api, categoryConfigs);
            List<ArtistData> finalArtists = applyFilteringPipeline(artistDataList);
            
            return saveArtistsToDatabase(finalArtists);
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("아티스트 시드 실패", e);
            throw new BusinessException(ArtistErrorCode.SPOTIFY_API_ERROR);
        }
    }
    
    private Map<String, CategoryConfig> createCategoryConfigs() {
        Map<String, CategoryConfig> categoryConfigs = new LinkedHashMap<>();
        categoryConfigs.put("아이돌(걸그룹/보이그룹)", new CategoryConfig(List.of(
                "k-pop girl group", "k-pop boy group", "k-pop group",
                "k-pop idol", "korean idol", "k-pop"
        ), 200, true));
        categoryConfigs.put("유명 솔로", new CategoryConfig(List.of(
                "k-pop solo", "korean solo", "k-pop singer",
                "korean ballad", "k-ballad"
        ), 60, true));
        categoryConfigs.put("밴드", new CategoryConfig(List.of(
                "korean band", "korean rock band", "k-indie band",
                "korean rock", "korean alternative", "korean punk",
                "korean indie rock"
        ), 80, true));
        categoryConfigs.put("힙합/인디/R&B", new CategoryConfig(List.of(
                "k-rap", "korean hip hop", "k-r&b", "korean r&b",
                "k-indie", "korean indie"
        ), 60, true));
        categoryConfigs.put("발라드/OST", new CategoryConfig(List.of(
                "k-ost", "korean ost"
        ), 60, true));
        categoryConfigs.put("글로벌", new CategoryConfig(List.of(
                "pop", "hip hop", "r&b", "rock", "alternative rock", 
                "rock band", "indie rock", "punk rock",
                "edm", "indie pop", "j-pop", "j-rock"
        ), 40, false));
        return categoryConfigs;
    }

    // Phase 1: FastSeed 모드 - 카테고리당 제한된 수집 (호출 폭발 방지)
    private List<ArtistData> collectArtistsRoundRobin(SpotifyApi api, Map<String, CategoryConfig> categoryConfigs) {
        List<ArtistData> artistDataList = new ArrayList<>();
        Set<String> collectedSpotifyIds = new HashSet<>(); // Spotify ID 중복 제거
        Set<String> seenNormalizedNames = new HashSet<>(); // 정규화된 이름 중복 제거
        
        Map<String, Integer> categoryKeywordIndex = initializeCategoryTracking(categoryConfigs);
        Map<String, Map<String, Integer>> categoryKeywordPageIndex = initializeCategoryPageTracking(categoryConfigs);
        Map<String, Integer> categoryCollectedCount = initializeCategoryCollectedCount(categoryConfigs);
        
        // 모든 카테고리를 순회하며 카테고리당 최대 50명씩 수집
        for (Map.Entry<String, CategoryConfig> entry : categoryConfigs.entrySet()) {
            String categoryName = entry.getKey();
            CategoryConfig config = entry.getValue();
            
            // 카테고리당 최대 수집 수 확인
            int collectedInCategory = categoryCollectedCount.get(categoryName);
            if (collectedInCategory >= MAX_ARTISTS_PER_CATEGORY) {
                continue;
            }
            
            int keywordIndex = categoryKeywordIndex.get(categoryName);
            if (keywordIndex >= config.keywords.size()) {
                continue;
            }
            
            String query = config.keywords.get(keywordIndex);
            Map<String, Integer> keywordPageIndex = categoryKeywordPageIndex.get(categoryName);
            int currentPage = keywordPageIndex.getOrDefault(query, 0);
            
            // 키워드당 최대 1페이지까지만 수집 (FastSeed)
            if (currentPage >= MAX_PAGES_PER_KEYWORD) {
                categoryKeywordIndex.put(categoryName, keywordIndex + 1);
                continue;
            }
            
            try {
                processSearchPage(api, query, currentPage, config, categoryName,
                        artistDataList, collectedSpotifyIds, seenNormalizedNames,
                        categoryKeywordIndex, categoryKeywordPageIndex, keywordPageIndex,
                        categoryCollectedCount);
            } catch (Exception e) {
                categoryKeywordIndex.put(categoryName, keywordIndex + 1);
            }
        }
        
        if (artistDataList.isEmpty()) {
            throw new BusinessException(ArtistErrorCode.ARTIST_SEED_FAILED);
        }
        
        // 배치로 DB 중복 제거
        return removeExistingArtists(artistDataList);
    }
    
    private Map<String, Integer> initializeCategoryCollectedCount(Map<String, CategoryConfig> categoryConfigs) {
        Map<String, Integer> count = new HashMap<>();
        for (String categoryName : categoryConfigs.keySet()) {
            count.put(categoryName, 0);
        }
        return count;
    }
    
    // 배치로 DB에 이미 존재하는 아티스트 제거
    private List<ArtistData> removeExistingArtists(List<ArtistData> candidates) {
        if (candidates.isEmpty()) {
            return candidates;
        }
        
        List<String> spotifyIds = candidates.stream()
                .map(data -> data.spotifyId)
                .filter(Objects::nonNull)
                .collect(toList());
        
        if (spotifyIds.isEmpty()) {
            return candidates;
        }
        
        // 배치로 DB에서 존재하는 spotifyId 조회
        List<String> existingSpotifyIds = artistRepository.findSpotifyIdsBySpotifyIdsIn(spotifyIds);
        Set<String> existingSet = new HashSet<>(existingSpotifyIds);
        
        // 존재하지 않는 아티스트만 반환
        return candidates.stream()
                .filter(data -> !existingSet.contains(data.spotifyId))
                .collect(toList());
    }

    private Map<String, Integer> initializeCategoryTracking(Map<String, CategoryConfig> categoryConfigs) {
        Map<String, Integer> tracking = new HashMap<>();
        for (String categoryName : categoryConfigs.keySet()) {
            tracking.put(categoryName, 0);
        }
        return tracking;
    }

    private Map<String, Map<String, Integer>> initializeCategoryPageTracking(Map<String, CategoryConfig> categoryConfigs) {
        Map<String, Map<String, Integer>> pageTracking = new HashMap<>();
        for (String categoryName : categoryConfigs.keySet()) {
            pageTracking.put(categoryName, new HashMap<>());
        }
        return pageTracking;
    }

    private void processSearchPage(SpotifyApi api, String query, int currentPage, CategoryConfig config,
                                     String categoryName, List<ArtistData> artistDataList,
                                     Set<String> collectedSpotifyIds, Set<String> seenNormalizedNames,
                                     Map<String, Integer> categoryKeywordIndex,
                                     Map<String, Map<String, Integer>> categoryKeywordPageIndex,
                                     Map<String, Integer> keywordPageIndex,
                                     Map<String, Integer> categoryCollectedCount) throws Exception {
        spotifyRateLimiter.acquire();
        int offset = currentPage * SEARCH_LIMIT;
        
        Paging<se.michaelthelin.spotify.model_objects.specification.Artist> paging = api.searchArtists(query)
                .limit(SEARCH_LIMIT)
                .offset(offset)
                .build()
                .execute();

        var items = paging.getItems();
        if (items == null || items.length == 0) {
            categoryKeywordIndex.put(categoryName, categoryKeywordIndex.get(categoryName) + 1);
            return;
        }
        
        int collectedInCategory = categoryCollectedCount.get(categoryName);

        for (var spotifyArtist : items) {
            // 카테고리당 최대 수집 수 확인
            if (collectedInCategory >= MAX_ARTISTS_PER_CATEGORY) {
                break;
            }
            
            if (!shouldAddArtist(spotifyArtist, config, categoryName, collectedSpotifyIds, seenNormalizedNames)) {
                continue;
            }
            
            ArtistData artistData = createArtistData(spotifyArtist);
            artistDataList.add(artistData);
            collectedSpotifyIds.add(artistData.spotifyId);
            
            // 정규화된 이름도 추가
            String normalizedName = normalizeNameForDedup(artistData.name);
            if (normalizedName != null && !normalizedName.isBlank()) {
                seenNormalizedNames.add(normalizedName);
            }
            
            collectedInCategory++;
            categoryCollectedCount.put(categoryName, collectedInCategory);
        }
        
        keywordPageIndex.put(query, currentPage + 1);
        if (items.length < SEARCH_LIMIT || offset + SEARCH_LIMIT >= paging.getTotal()) {
            categoryKeywordIndex.put(categoryName, categoryKeywordIndex.get(categoryName) + 1);
        }
        
        Thread.sleep(API_CALL_DELAY_MS);
    }
    
    // 이름 정규화 (중복 제거용)
    private String normalizeNameForDedup(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.toLowerCase()
                .replaceAll("[\\s\\-_\\(\\)\\[\\]]", "")
                .trim();
    }

    // 카테고리별 필터 임계값 적용 (popularity + followers AND 조합)
    // FastSeed: 추가 API 호출 없이 popularity/followers만으로 판단
    private boolean shouldAddArtist(se.michaelthelin.spotify.model_objects.specification.Artist spotifyArtist,
                                   CategoryConfig config, String categoryName, 
                                   Set<String> collectedSpotifyIds, Set<String> seenNormalizedNames) {
        String spotifyId = spotifyArtist.getId();
        String name = spotifyArtist.getName();
        
        // Spotify ID 중복 체크
        if (spotifyId == null || name == null || name.isBlank() || collectedSpotifyIds.contains(spotifyId)) {
            return false;
        }
        
        // 정규화된 이름 중복 체크
        String normalizedName = normalizeNameForDedup(name);
        if (normalizedName != null && !normalizedName.isBlank() && seenNormalizedNames.contains(normalizedName)) {
            return false;
        }
        
        if (isSurvivalProgramGroup(name) || isCompilationArtist(name)) {
            return false;
        }
        
        if (config.requireKorean && !isLikelyKoreanMusic(spotifyArtist)) {
            return false;
        }
        
        Integer popularity = spotifyArtist.getPopularity();
        Integer followers = spotifyArtist.getFollowers() != null 
                ? spotifyArtist.getFollowers().getTotal() 
                : null;
        
        // 카테고리별 필터 임계값 적용 (추가 API 호출 없이)
        if (!meetsCategoryThreshold(categoryName, popularity, followers)) {
            return false;
        }
        
        String imageUrl = pickImageUrl(spotifyArtist.getImages());
        return imageUrl != null && !imageUrl.isBlank();
    }

    // 카테고리별 임계값 확인 (popularity AND followers 조합)
    private boolean meetsCategoryThreshold(String categoryName, Integer popularity, Integer followers) {
        if (popularity == null) {
            popularity = 0;
        }
        if (followers == null) {
            followers = 0;
        }
        
        switch (categoryName) {
            case "아이돌(걸그룹/보이그룹)":
                // popularity >= 65 → 무조건 통과
                if (popularity >= IDOL_UNCONDITIONAL_POPULARITY) {
                    return true;
                }
                // popularity < 25 AND followers < 5,000 → 제외
                return !(popularity < IDOL_MIN_POPULARITY && followers < IDOL_MIN_FOLLOWERS);
                
            case "힙합/인디/R&B":
                // popularity >= 55 → 강통과
                if (popularity >= HIPHOP_UNCONDITIONAL_POPULARITY) {
                    return true;
                }
                // popularity < 20 AND followers < 3,000 → 제외
                return !(popularity < HIPHOP_MIN_POPULARITY && followers < HIPHOP_MIN_FOLLOWERS);
                
            case "밴드":
                // popularity < 15 AND followers < 1,500 → 제외
                return !(popularity < BAND_MIN_POPULARITY && followers < BAND_MIN_FOLLOWERS);
                
            case "발라드/OST":
                // popularity < 20 AND followers < 2,000 → 제외
                return !(popularity < BALLAD_MIN_POPULARITY && followers < BALLAD_MIN_FOLLOWERS);
                
            case "글로벌":
                // popularity < 15 AND followers < 20,000 → 제외
                return !(popularity < GLOBAL_MIN_POPULARITY && followers < GLOBAL_MIN_FOLLOWERS);
                
            case "유명 솔로":
                // 아이돌과 동일한 기준 적용
                if (popularity >= IDOL_UNCONDITIONAL_POPULARITY) {
                    return true;
                }
                return !(popularity < IDOL_MIN_POPULARITY && followers < IDOL_MIN_FOLLOWERS);
                
            default:
                // 기본값: 아이돌 기준
                if (popularity >= IDOL_UNCONDITIONAL_POPULARITY) {
                    return true;
                }
                return !(popularity < IDOL_MIN_POPULARITY && followers < IDOL_MIN_FOLLOWERS);
        }
    }

    private ArtistData createArtistData(se.michaelthelin.spotify.model_objects.specification.Artist spotifyArtist) {
        String spotifyId = spotifyArtist.getId();
        String name = spotifyArtist.getName();
        String[] genres = spotifyArtist.getGenres();
        Integer popularity = spotifyArtist.getPopularity();
        Integer followers = spotifyArtist.getFollowers() != null 
                ? spotifyArtist.getFollowers().getTotal() 
                : null;
        String imageUrl = pickImageUrl(spotifyArtist.getImages());
        String artistTypeStr = inferArtistType(spotifyArtist);
        ArtistType artistType = ArtistType.valueOf(artistTypeStr);

        List<String> genreList = genres != null 
                ? Arrays.stream(genres).filter(Objects::nonNull).filter(g -> !g.isBlank()).collect(toList())
                : List.of();

        return new ArtistData(spotifyId, name.trim(), artistType, imageUrl, genreList, popularity, followers);
    }

    private boolean isSurvivalProgramGroup(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        
        String lowerName = name.toLowerCase();
        
        for (String keyword : SURVIVAL_PROGRAM_KEYWORDS) {
            if (lowerName.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean isCompilationArtist(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        
        String lowerName = name.toLowerCase();
        String trimmedName = name.trim();
        String normalizedName = trimmedName.replaceAll("\\s+", "");
        
        for (String keyword : COMPILATION_KEYWORDS) {
            if (lowerName.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        
        if (trimmedName.matches(".*[가-힣]+\\s+(가수들|싱어들|아티스트들|밴드들|그룹들)$")) {
            return true;
        }
        
        String[] genreKeywords = {"발라드", "힙합", "인디", "록", "팝", "k-pop", "kpop", "k-rap", "krap", "r&b", "rnb"};
        String[] artistSuffixes = {"싱어", "가수", "아티스트", "밴드", "그룹"};
        
        for (String genre : genreKeywords) {
            for (String suffix : artistSuffixes) {
                if (normalizedName.contains(genre.toLowerCase() + suffix) || 
                    normalizedName.contains(genre + suffix)) {
                    if (trimmedName.length() <= 20) {
                        return true;
                    }
                }
            }
        }
        
        if (lowerName.matches(".*\\s+(singers|artists|bands|groups)$")) {
            String[] genrePrefixes = {"ballad", "hip hop", "hip-hop", "r&b", "rnb", "indie", "rock", "pop", "k-pop", "korean"};
            for (String prefix : genrePrefixes) {
                if (lowerName.startsWith(prefix + " ") && lowerName.endsWith("s")) {
                    return true;
                }
            }
        }
        
        String[] englishGenrePrefixes = {"ballad", "hiphop", "hip-hop", "r&b", "rnb", "indie", "rock", "pop", "kpop", "korean"};
        String[] englishSuffixes = {"singer", "singers", "artist", "artists", "band", "bands", "group", "groups"};
        String normalizedLower = lowerName.replaceAll("\\s+", "");
        
        for (String prefix : englishGenrePrefixes) {
            for (String suffix : englishSuffixes) {
                if (normalizedLower.contains(prefix + suffix) && trimmedName.length() <= 30) {
                    return true;
                }
            }
        }
        
        return false;
    }

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

    // 5단계 필터링 파이프라인: 국내 우선, 해외 쿼터제, Fallback
    private List<ArtistData> applyFilteringPipeline(List<ArtistData> artistDataList) {
        // 1단계: 기본 필터링
        List<ArtistData> filteredByDb = filterByDbInfo(artistDataList);
        List<ArtistData> filteredByUnit = filterUnitExclude(filteredByDb);
        
        // 2단계: 국내/해외/Unknown 분류
        KoreanClassificationResult classification = classifyKoreanArtists(filteredByUnit);
        
        // 3단계: 국내를 먼저 목표 수까지 채우기
        List<ArtistData> selected = selectKoreanArtists(classification);
        Set<String> selectedSpotifyIds = selected.stream()
                .map(data -> data.spotifyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        // 4단계: 해외는 기준 완화 + 쿼터제 적용
        List<ArtistData> globalCandidates = classification.globalArtists.stream()
                .filter(data -> !selectedSpotifyIds.contains(data.spotifyId))
                .collect(toList());
        List<ArtistData> globalArtists = selectGlobalArtists(globalCandidates, selected.size());
        selected.addAll(globalArtists);
        selectedSpotifyIds.addAll(globalArtists.stream()
                .map(data -> data.spotifyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        
        // 5단계: Fallback - 300명이 안 차면 부족분 채우기
        if (selected.size() < MAX_SEED_COUNT) {
            List<ArtistData> fallbackArtists = fillRemainingSlots(
                    classification, selected.size(), selectedSpotifyIds);
            selected.addAll(fallbackArtists);
        }
        
        // 최종 300명 제한
        if (selected.size() > MAX_SEED_COUNT) {
            return selected.stream()
                    .sorted((a1, a2) -> {
                        double score1 = calculateConcertScore(a1.popularity, a1.followers);
                        double score2 = calculateConcertScore(a2.popularity, a2.followers);
                        return Double.compare(score2, score1);
                    })
                    .limit(MAX_SEED_COUNT)
                    .collect(toList());
        }
        
        return selected;
    }
    
    /**
     * 국내/해외/Unknown 분류 결과
     */
    private static class KoreanClassificationResult {
        final List<ArtistData> strongKorean; // 확정 국내 (k-* 장르)
        final List<ArtistData> weakKorean;   // 가능 국내 (한글 포함)
        final List<ArtistData> globalArtists; // 해외
        final List<ArtistData> unknown;      // 애매한 경우
        
        KoreanClassificationResult(List<ArtistData> strongKorean, List<ArtistData> weakKorean,
                                   List<ArtistData> globalArtists, List<ArtistData> unknown) {
            this.strongKorean = strongKorean;
            this.weakKorean = weakKorean;
            this.globalArtists = globalArtists;
            this.unknown = unknown;
        }
    }
    
    /**
     * 2단계: 국내/해외/Unknown 분류
     */
    private KoreanClassificationResult classifyKoreanArtists(List<ArtistData> candidates) {
        List<ArtistData> strongKorean = new ArrayList<>();
        List<ArtistData> weakKorean = new ArrayList<>();
        List<ArtistData> globalArtists = new ArrayList<>();
        List<ArtistData> unknown = new ArrayList<>();
        
        for (ArtistData data : candidates) {
            KoreanType koreanType = classifyKoreanType(data);
            
            switch (koreanType) {
                case STRONG_KOREAN:
                    strongKorean.add(data);
                    break;
                case WEAK_KOREAN:
                    weakKorean.add(data);
                    break;
                case GLOBAL:
                    globalArtists.add(data);
                    break;
                case UNKNOWN:
                    unknown.add(data);
                    break;
            }
        }
        
        return new KoreanClassificationResult(strongKorean, weakKorean, globalArtists, unknown);
    }
    
    /**
     * 국내 타입 분류
     */
    private enum KoreanType {
        STRONG_KOREAN,  // 확정 국내 (k-* 장르)
        WEAK_KOREAN,    // 가능 국내 (한글 포함)
        GLOBAL,         // 해외
        UNKNOWN         // 애매한 경우
    }
    
    /**
     * 아티스트의 국내 타입 분류
     */
    private KoreanType classifyKoreanType(ArtistData data) {
        // Strong Korean: genres에 k-* 장르 포함
        if (data.genres != null && !data.genres.isEmpty()) {
            String genresStr = String.join(" ", data.genres).toLowerCase();
            if (genresStr.contains("k-pop") || genresStr.contains("k-rap") || 
                genresStr.contains("k-r&b") || genresStr.contains("k-indie") ||
                genresStr.contains("k-rock") || genresStr.contains("k-ballad") ||
                genresStr.contains("korean")) {
                return KoreanType.STRONG_KOREAN;
            }
        }
        
        // Weak Korean: 이름에 한글 포함
        if (data.name != null && data.name.matches(".*[가-힣].*")) {
            return KoreanType.WEAK_KOREAN;
        }
        
        // Global: 명확히 해외 장르
        if (data.genres != null && !data.genres.isEmpty()) {
            String genresStr = String.join(" ", data.genres).toLowerCase();
            // 해외 장르가 있고, k-* 또는 korean이 없으면 해외
            if ((genresStr.contains("pop") || genresStr.contains("hip hop") || 
                 genresStr.contains("rock") || genresStr.contains("r&b") ||
                 genresStr.contains("indie") || genresStr.contains("alternative")) &&
                !genresStr.contains("k-") && !genresStr.contains("korean")) {
                return KoreanType.GLOBAL;
            }
        }
        
        // Unknown: 애매한 경우 (국내 가능성 있음)
        return KoreanType.UNKNOWN;
    }
    
    /**
     * 3단계: 국내를 먼저 목표 수까지 채우기
     */
    private List<ArtistData> selectKoreanArtists(KoreanClassificationResult classification) {
        List<ArtistData> selected = new ArrayList<>();
        
        // Strong Korean 먼저 (가점, 인기순 정렬)
        List<ArtistData> strongSorted = classification.strongKorean.stream()
                .sorted((a1, a2) -> {
                    double score1 = calculateConcertScore(a1.popularity, a1.followers);
                    double score2 = calculateConcertScore(a2.popularity, a2.followers);
                    return Double.compare(score2, score1);
                })
                .collect(toList());
        
        // Strong Korean을 목표 수까지 추가
        int strongCount = Math.min(strongSorted.size(), TARGET_KOREAN_COUNT);
        selected.addAll(strongSorted.stream()
                .limit(strongCount)
                .collect(toList()));
        
        // Weak Korean 추가 (부족하면)
        if (selected.size() < TARGET_KOREAN_COUNT) {
            List<ArtistData> weakSorted = classification.weakKorean.stream()
                    .sorted((a1, a2) -> {
                        double score1 = calculateConcertScore(a1.popularity, a1.followers);
                        double score2 = calculateConcertScore(a2.popularity, a2.followers);
                        return Double.compare(score2, score1);
                    })
                    .collect(toList());
            
            int remaining = TARGET_KOREAN_COUNT - selected.size();
            selected.addAll(weakSorted.stream()
                    .limit(remaining)
                    .collect(toList()));
        }
        
        return selected;
    }
    
    /**
     * 4단계: 해외는 기준 완화 + 쿼터제 적용
     */
    private List<ArtistData> selectGlobalArtists(List<ArtistData> globalCandidates, int currentCount) {
        if (globalCandidates.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 해외 쿼터 계산 (최종 목표 300명 기준으로 고정)
        int globalQuota = Math.min(
                MAX_GLOBAL_COUNT,
                (int) Math.ceil(MAX_SEED_COUNT * GLOBAL_QUOTA_PERCENT / 100.0)
        );
        
        if (globalQuota <= 0) {
            return new ArrayList<>();
        }
        
        // 해외 기준 완화: popularity 또는 followers가 충분하면 통과
        List<ArtistData> qualified = globalCandidates.stream()
                .filter(data -> {
                    Integer pop = data.popularity != null ? data.popularity : 0;
                    Integer fol = data.followers != null ? data.followers : 0;
                    
                    // 기준 완화: popularity >= 30 또는 followers >= 50,000 (내한 가능한 중상위급)
                    return pop >= 30 || fol >= 50_000;
                })
                .sorted((a1, a2) -> {
                    double score1 = calculateConcertScore(a1.popularity, a1.followers);
                    double score2 = calculateConcertScore(a2.popularity, a2.followers);
                    return Double.compare(score2, score1);
                })
                .limit(globalQuota)
                .collect(toList());
        
        return qualified;
    }
    
    /**
     * 5단계: Fallback - 부족분 채우기
     */
    private List<ArtistData> fillRemainingSlots(KoreanClassificationResult classification, 
                                                int currentCount, Set<String> selectedSpotifyIds) {
        int remaining = MAX_SEED_COUNT - currentCount;
        if (remaining <= 0) {
            return new ArrayList<>();
        }
        
        List<ArtistData> fallback = new ArrayList<>();
        
        // 1순위: 국내 후보 남은 애들 (strong + weak, 이미 선택된 것 제외)
        List<ArtistData> remainingKorean = new ArrayList<>();
        remainingKorean.addAll(classification.strongKorean.stream()
                .filter(data -> !selectedSpotifyIds.contains(data.spotifyId))
                .collect(toList()));
        remainingKorean.addAll(classification.weakKorean.stream()
                .filter(data -> !selectedSpotifyIds.contains(data.spotifyId))
                .collect(toList()));
        
        List<ArtistData> koreanSorted = remainingKorean.stream()
                .sorted((a1, a2) -> {
                    double score1 = calculateConcertScore(a1.popularity, a1.followers);
                    double score2 = calculateConcertScore(a2.popularity, a2.followers);
                    return Double.compare(score2, score1);
                })
                .limit(remaining)
                .collect(toList());
        
        fallback.addAll(koreanSorted);
        selectedSpotifyIds.addAll(koreanSorted.stream()
                .map(data -> data.spotifyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        remaining -= fallback.size();
        
        // 2순위: Unknown (국내 가능성 있는 애들, 이미 선택된 것 제외)
        if (remaining > 0 && !classification.unknown.isEmpty()) {
            List<ArtistData> unknownSorted = classification.unknown.stream()
                    .filter(data -> !selectedSpotifyIds.contains(data.spotifyId))
                    .sorted((a1, a2) -> {
                        double score1 = calculateConcertScore(a1.popularity, a1.followers);
                        double score2 = calculateConcertScore(a2.popularity, a2.followers);
                        return Double.compare(score2, score1);
                    })
                    .limit(remaining)
                    .collect(toList());
            
            fallback.addAll(unknownSorted);
            selectedSpotifyIds.addAll(unknownSorted.stream()
                    .map(data -> data.spotifyId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet()));
            remaining -= unknownSorted.size();
        }
        
        // 3순위: 해외 컷 완화 (한 단계 더 낮춤, 이미 선택된 것 제외)
        if (remaining > 0 && !classification.globalArtists.isEmpty()) {
            List<ArtistData> relaxedGlobal = classification.globalArtists.stream()
                    .filter(data -> !selectedSpotifyIds.contains(data.spotifyId))
                    .filter(data -> {
                        Integer pop = data.popularity != null ? data.popularity : 0;
                        Integer fol = data.followers != null ? data.followers : 0;
                        
                        // 한 단계 더 완화: popularity >= 20 또는 followers >= 20,000
                        return pop >= 20 || fol >= 20_000;
                    })
                    .sorted((a1, a2) -> {
                        double score1 = calculateConcertScore(a1.popularity, a1.followers);
                        double score2 = calculateConcertScore(a2.popularity, a2.followers);
                        return Double.compare(score2, score1);
                    })
                    .limit(remaining)
                    .collect(toList());
            
            fallback.addAll(relaxedGlobal);
        }
        
        return fallback;
    }
    
    // 콘서트 중심 스코어: popularity * 1.0 + log10(followers+1) * 15
    private double calculateConcertScore(Integer popularity, Integer followers) {
        double popScore = (popularity != null && popularity >= 0) ? popularity * 1.0 : 0.0;
        
        double followerScore = 0.0;
        if (followers != null && followers > 0) {
            followerScore = Math.log10(followers + 1) * 15.0;
        }
        
        return popScore + followerScore;
    }
    
    // 장르로 카테고리 추론
    private String inferCategoryFromGenres(List<String> genres) {
        if (genres == null || genres.isEmpty()) {
            return "유명 솔로";
        }
        
        String genresStr = String.join(" ", genres).toLowerCase();
        
        // 아이돌
        if (genresStr.contains("k-pop") && (genresStr.contains("girl group") || 
            genresStr.contains("boy group") || genresStr.contains("idol"))) {
            return "아이돌(걸그룹/보이그룹)";
        }
        
        // 힙합/R&B
        if (genresStr.contains("hip hop") || genresStr.contains("k-rap") || 
            genresStr.contains("k-r&b") || genresStr.contains("r&b")) {
            return "힙합/인디/R&B";
        }
        
        // 밴드
        if (genresStr.contains("rock") || genresStr.contains("band") || 
            genresStr.contains("indie") || genresStr.contains("alternative")) {
            return "밴드";
        }
        
        // 발라드/OST
        if (genresStr.contains("ballad") || genresStr.contains("ost") || 
            genresStr.contains("k-ballad")) {
            return "발라드/OST";
        }
        
        // 글로벌
        if (!genresStr.contains("k-pop") && !genresStr.contains("korean")) {
            return "글로벌";
        }
        
        return "유명 솔로";
    }
    
    private int saveArtistsToDatabase(List<ArtistData> finalArtists) {
        Map<String, Artist> artistMap = upsertArtists(finalArtists);
        Map<String, Genre> genreMap = processGenres(finalArtists);
        int totalMappings = createArtistGenreMappings(finalArtists, artistMap, genreMap);

        log.info("아티스트 시드 완료: 아티스트 {}명, 장르 {}개, 매핑 {}개", 
                finalArtists.size(), genreMap.size(), totalMappings);

        return finalArtists.size();
    }
    
    // Bulk 저장: 기존 아티스트를 한 번에 조회하고, 변경/신규 목록을 saveAll로 저장
    private Map<String, Artist> upsertArtists(List<ArtistData> finalArtists) {
        if (finalArtists.isEmpty()) {
            return new HashMap<>();
        }
        
        // 1. 모든 spotifyId 수집
        List<String> spotifyIds = finalArtists.stream()
                .map(data -> data.spotifyId)
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .collect(toList());
        
        // 2. 기존 아티스트를 한 번에 조회
        Map<String, Artist> existingArtistMap = new HashMap<>();
        if (!spotifyIds.isEmpty()) {
            List<Artist> existingArtists = artistRepository.findBySpotifyArtistIdIn(spotifyIds);
            for (Artist artist : existingArtists) {
                if (artist.getSpotifyArtistId() != null) {
                    existingArtistMap.put(artist.getSpotifyArtistId(), artist);
                }
            }
        }
        
        // 3. 변경/신규 목록 생성 (메모리에서 upsert 판단)
        List<Artist> toSave = new ArrayList<>();
        Map<String, Artist> artistMap = new HashMap<>();
        
        for (ArtistData data : finalArtists) {
            Artist artist = existingArtistMap.get(data.spotifyId);
            
            if (artist != null) {
                // 기존 아티스트 업데이트
                artist.setArtistName(data.name);
                artist.setArtistType(data.artistType);
                artist.setImageUrl(data.imageUrl);
                artist.getArtistGenres().clear();
            } else {
                // 신규 아티스트 생성
                artist = new Artist(data.spotifyId, data.name, null, data.artistType);
                artist.setImageUrl(data.imageUrl);
            }
            
            toSave.add(artist);
            artistMap.put(data.spotifyId, artist);
        }
        
        // 4. Bulk 저장 (한 번에 저장)
        if (!toSave.isEmpty()) {
            artistRepository.saveAll(toSave);
        }
        
        return artistMap;
    }
    
    private Map<String, Genre> processGenres(List<ArtistData> finalArtists) {
        // 1단계: 원본 장르명들을 통합된 카테고리로 변환
        Set<String> unifiedGenreNames = finalArtists.stream()
                    .flatMap(data -> data.genres.stream())
                    .filter(Objects::nonNull)
                    .filter(g -> !g.isBlank())
                    .map(this::normalizeGenre)
                    .filter(Objects::nonNull)
                    .filter(g -> !g.isBlank())
                    .collect(Collectors.toSet());

            if (unifiedGenreNames.isEmpty()) {
                return new HashMap<>();
            }

            // 2단계: 통합된 장르명으로 DB에서 조회
            List<Genre> existingGenres = genreRepository.findByGenreNameIn(new ArrayList<>(unifiedGenreNames));
            Set<String> existingGenreNames = existingGenres.stream()
                    .map(Genre::getGenreName)
                    .collect(Collectors.toSet());

            // 3단계: 신규 장르 생성
            List<String> newGenreNames = unifiedGenreNames.stream()
                    .filter(name -> !existingGenreNames.contains(name))
                    .collect(toList());

            if (!newGenreNames.isEmpty()) {
                List<Genre> newGenres = newGenreNames.stream()
                        .map(Genre::new)
                        .collect(toList());
                genreRepository.saveAll(newGenres);
                existingGenres.addAll(newGenres);
            }

        return existingGenres.stream()
                    .collect(Collectors.toMap(Genre::getGenreName, g -> g, (g1, g2) -> g1));
    }
    
    /**
     * 원본 장르명을 통합된 카테고리로 변환
     * 우선순위 순서대로 체크하여 매칭되는 첫 번째 카테고리 반환
     */
    private String normalizeGenre(String originalGenre) {
        if (originalGenre == null || originalGenre.isBlank()) {
            return null;
        }
        
        String lowerGenre = originalGenre.toLowerCase().trim();
        
        // 1순위: KOREAN (k-로 시작)
        if (lowerGenre.startsWith("k-")) {
            return "KOREAN";
        }
        
        // 2순위: HIPHOP/RAP
        if (containsAny(lowerGenre, "hip hop", "rap", "drill", "grime", "boom bap", "hip-hop", "hiphop")) {
            return "HIPHOP/RAP";
        }
        
        // 3순위: R&B/SOUL
        if (containsAny(lowerGenre, "r&b", "rnb", "soul", "r and b")) {
            return "R&B/SOUL";
        }
        
        // 4순위: METAL
        if (lowerGenre.contains("metal")) {
            return "METAL";
        }
        
        // 5순위: ROCK
        if (containsAny(lowerGenre, "rock", "grunge", "shoegaze", "britpop", "classic rock")) {
            return "ROCK";
        }
        
        // 6순위: INDIE/ALT
        if (containsAny(lowerGenre, "indie", "alternative", "art rock", "neo-psychedelic", "jangle pop", "alt")) {
            return "INDIE/ALT";
        }
        
        // 7순위: LATIN
        if (containsAny(lowerGenre, "latin", "reggaeton", "urbano", "bachata", "latin afrobeats")) {
            return "LATIN";
        }
        
        // 8순위: REGGAE
        if (lowerGenre.contains("reggae")) {
            return "REGGAE";
        }
        
        // 9순위: JAPAN
        if (containsAny(lowerGenre, "j-pop", "j-rock", "jpop", "jrock", "vocaloid", "shibuya-kei", "japanese", "city pop", "japanese indie")) {
            return "JAPAN";
        }
        
        // 10순위: SOUNDTRACK/ANIME
        if (containsAny(lowerGenre, "soundtrack", "anime", "bollywood", "tollywood", "kollywood", "ost")) {
            return "SOUNDTRACK/ANIME";
        }
        
        // 11순위: POP (pop이 포함된 경우)
        if (lowerGenre.contains("pop")) {
            return "POP";
        }
        
        // 12순위: ETC (그 외 모든 경우)
        return "ETC";
    }
    
    /**
     * 문자열이 주어진 키워드들 중 하나라도 포함하는지 확인
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    // Batch 저장: ArtistGenre를 별도로 수집하여 saveAll로 한 번에 저장
    private int createArtistGenreMappings(List<ArtistData> finalArtists, Map<String, Artist> artistMap,
                                         Map<String, Genre> genreMap) {
        List<ArtistGenre> artistGenresToSave = new ArrayList<>();
        Set<String> seenMappings = new HashSet<>(); // 중복 매핑 방지
        
        for (ArtistData data : finalArtists) {
            Artist artist = artistMap.get(data.spotifyId);
            if (artist == null) {
                continue;
            }

            for (String originalGenreName : data.genres) {
                if (originalGenreName == null || originalGenreName.isBlank()) {
                    continue;
                }
                
                // 원본 장르명을 통합된 카테고리로 변환
                String unifiedGenreName = normalizeGenre(originalGenreName);
                if (unifiedGenreName == null || unifiedGenreName.isBlank()) {
                    continue;
                }
                
                Genre genre = genreMap.get(unifiedGenreName);
                if (genre == null) {
                    continue;
                }
                
                // 중복 매핑 체크 (artistId-genreId 조합)
                String mappingKey = artist.getId() + "-" + genre.getId();
                if (seenMappings.contains(mappingKey)) {
                    continue;
                }
                
                ArtistGenre artistGenre = new ArtistGenre(artist, genre);
                artistGenresToSave.add(artistGenre);
                artist.getArtistGenres().add(artistGenre);
                seenMappings.add(mappingKey);
            }
        }
        
        // Batch 저장 (한 번에 저장)
        if (!artistGenresToSave.isEmpty()) {
            // ArtistGenre는 Artist의 cascade로 저장되므로, Artist를 다시 저장할 필요 없음
            // 하지만 명시적으로 저장하려면 ArtistGenreRepository가 필요함
            // 현재는 cascade로 처리되므로 Artist 저장은 upsertArtists에서 이미 완료됨
        }

        return artistGenresToSave.size();
    }

    // 기본 필터: 이름, 이미지, 기본 임계값 확인
    private List<ArtistData> filterByDbInfo(List<ArtistData> candidates) {
        return candidates.stream()
                .filter(data -> {
                    if (data.name == null || data.name.isBlank()) {
                        return false;
                    }
                    
                    if (data.imageUrl == null || data.imageUrl.isBlank()) {
                        return false;
                    }
                    
                    // 카테고리별 필터는 이미 수집 단계에서 적용됨
                    // 여기서는 기본 검증만 수행
                    if (data.popularity == null || data.popularity < 0) {
                        return false;
                    }
                    
                    return true;
                })
                .collect(toList());
    }
    
    // 유닛/프로젝트 그룹 제외: 1차 이름 패턴, 2차 MusicBrainz (애매한 경우만)
    private List<ArtistData> filterUnitExclude(List<ArtistData> candidates) {
        List<ArtistData> activeArtists = new ArrayList<>();
        List<ArtistData> ambiguousCandidates = new ArrayList<>();
        
        // 1차: 이름 패턴으로 빠르게 컷
        for (ArtistData data : candidates) {
            if (isUnitNamePattern(data.name)) {
                // 명확한 유닛 패턴이면 제외
                continue;
            }
            
            // 애매한 경우만 MusicBrainz 확인 대상에 추가
            if (isAmbiguousUnitName(data.name)) {
                ambiguousCandidates.add(data);
            } else {
                // 명확히 유닛이 아니면 통과
                activeArtists.add(data);
            }
        }
        
        // 2차: 애매한 경우만 MusicBrainz로 확인
        for (ArtistData data : ambiguousCandidates) {
            musicBrainzRateLimiter.acquire();
            
            boolean isUnit = false;
            try {
                Optional<com.back.web7_9_codecrete_be.global.musicbrainz.MusicBrainzClient.ArtistInfo> artistInfoOpt = 
                        musicBrainzClient.searchArtist(data.name);
                
                if (artistInfoOpt.isPresent()) {
                    String mbid = artistInfoOpt.get().getMbid();
                    if (mbid != null && !mbid.isBlank()) {
                        Optional<Boolean> isSubunitOpt = musicBrainzClient.isSubunitOrProjectGroup(mbid);
                        if (isSubunitOpt.isPresent() && isSubunitOpt.get()) {
                            isUnit = true;
                        }
                    }
                }
            } catch (Exception e) {
                // MusicBrainz 조회 실패는 통과 (보수적 접근)
            }
            
            if (!isUnit) {
                activeArtists.add(data);
            }
        }
        
        return activeArtists;
    }
    
    // 애매한 유닛 이름인지 확인 (MusicBrainz 확인이 필요한 경우)
    private boolean isAmbiguousUnitName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        
        String lowerName = name.toLowerCase();
        
        // 하이픈이 있지만 패턴이 명확하지 않은 경우
        if (lowerName.contains("-") && !isUnitNamePattern(name)) {
            // 예: "Artist-Name" 같은 일반적인 하이픈 사용은 제외
            // 하지만 "Group-Subunit" 같은 패턴은 이미 isUnitNamePattern에서 잡힘
            return false;
        }
        
        // 괄호가 있지만 패턴이 명확하지 않은 경우
        if (name.contains("(") && name.contains(")") && !isUnitNamePattern(name)) {
            // 예: "Artist (feat. Someone)" 같은 경우는 제외
            return false;
        }
        
        // 대부분의 경우는 이름 패턴에서 처리되므로, 애매한 경우는 거의 없음
        return false;
    }
    
    // 1차: 이름 패턴으로 빠르게 컷 (API 호출 0)
    private boolean isUnitNamePattern(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        
        String normalizedName = name.trim();
        String lowerName = normalizedName.toLowerCase();
        
        // 하이픈 패턴: .*-.+ (그룹명-유닛명)
        if (normalizedName.matches(".*[-–—].+")) {
            String[] parts = normalizedName.split("[-–—]");
            if (parts.length >= 2) {
                String suffix = parts[parts.length - 1].trim();
                // EXO-K, Girls' Generation-TTS 같은 패턴
                if (suffix.length() <= 20) {
                    return true;
                }
            }
        }
        
        // 괄호 패턴: .+\s*\(.+\) (괄호로 파생)
        if (normalizedName.matches(".+\\s*\\(.+\\)")) {
            int lastParen = normalizedName.lastIndexOf('(');
            int lastCloseParen = normalizedName.lastIndexOf(')');
            if (lastParen > 0 && lastCloseParen > lastParen) {
                String insideParen = normalizedName.substring(lastParen + 1, lastCloseParen).trim();
                // DAY6 (Even of Day) 같은 패턴
                if (insideParen.length() <= 30) {
                    return true;
                }
            }
        }
        
        // 키워드 포함: unit, subunit, project, collab, special, OST project
        String[] unitKeywords = {
            "unit", "subunit", "sub-unit", "sub group", "subgroup",
            "project", "project group", "collab", "collaboration",
            "special unit", "special stage", "ost project"
        };
        for (String keyword : unitKeywords) {
            if (lowerName.contains(keyword)) {
                return true;
            }
        }
        
        // 명시적 프로젝트 그룹
        String[] projectNames = {
            "got the beat", "super m", "superm", "k/da", "kda",
            "k-pop demon hunters cast", "demon hunters cast",
            "girls on top", "girlsontop"
        };
        for (String projectName : projectNames) {
            if (lowerName.contains(projectName)) {
                return true;
            }
        }
        
        return false;
    }
    
    private static class MusicBrainzFilterResult {
        private final List<ArtistData> activeArtists;
        private final List<ArtistData> mbFailedArtists;
        
        MusicBrainzFilterResult(List<ArtistData> activeArtists, List<ArtistData> mbFailedArtists) {
            this.activeArtists = activeArtists;
            this.mbFailedArtists = mbFailedArtists;
        }
        
        List<ArtistData> getActiveArtists() {
            return activeArtists;
        }
        
        List<ArtistData> getMbFailedArtists() {
            return mbFailedArtists;
        }
    }
    
    private MusicBrainzFilterResult filterByMusicBrainzEnded(List<ArtistData> candidates) {
        List<ArtistData> activeArtists = new ArrayList<>();
        List<ArtistData> mbFailedArtists = new ArrayList<>();
        
        final double UNCONDITIONAL_SCORE = calculatePopularityScore(UNCONDITIONAL_POPULARITY, UNCONDITIONAL_FOLLOWERS);
        final double KOREAN_LEGACY_SCORE = calculatePopularityScore(KOREAN_LEGACY_POPULARITY, KOREAN_LEGACY_FOLLOWERS);
        final double GLOBAL_LEGACY_SCORE = calculatePopularityScore(GLOBAL_LEGACY_POPULARITY, GLOBAL_LEGACY_FOLLOWERS);
        
        for (ArtistData data : candidates) {
            double score = calculatePopularityScore(data.popularity, data.followers);
            
            if (score >= UNCONDITIONAL_SCORE) {
                activeArtists.add(data);
                continue;
            }
            
            musicBrainzRateLimiter.acquire();
            
            Boolean isEnded = null;
            boolean mbSucceeded = false;
            
            try {
                Optional<com.back.web7_9_codecrete_be.global.musicbrainz.MusicBrainzClient.ArtistInfo> artistInfoOpt = 
                        musicBrainzClient.searchArtist(data.name);
                
                if (artistInfoOpt.isPresent()) {
                    String mbid = artistInfoOpt.get().getMbid();
                    if (mbid != null && !mbid.isBlank()) {
                        Optional<Boolean> endedOpt = musicBrainzClient.isEnded(mbid);
                        if (endedOpt.isPresent()) {
                            isEnded = endedOpt.get();
                            mbSucceeded = true;
                        }
                    }
                }
            } catch (Exception e) {
                // MusicBrainz 조회 실패는 Wikidata로 확인 대상에 추가
            }
            
            if (!mbSucceeded) {
                boolean isVeryLowPopularity = (data.popularity == null || data.popularity < MB_FAILED_MIN_POPULARITY) && 
                                             (data.followers == null || data.followers < MB_FAILED_MIN_FOLLOWERS);
                if (!isVeryLowPopularity) {
                    activeArtists.add(data);
                } else {
                    mbFailedArtists.add(data);
                }
                continue;
            }
            
            if (isEnded != null && !isEnded) {
                activeArtists.add(data);
                continue;
            }
            
            if (isEnded != null && isEnded) {
                boolean isKorean = data.genres.stream()
                        .anyMatch(g -> g != null && (g.toLowerCase().contains("k-pop") || 
                                g.toLowerCase().contains("korean")));
                
                double legacyThreshold = isKorean ? KOREAN_LEGACY_SCORE : GLOBAL_LEGACY_SCORE;
                
                if (score >= legacyThreshold) {
                    activeArtists.add(data);
                }
                continue;
            }
            
            activeArtists.add(data);
        }
        
        return new MusicBrainzFilterResult(activeArtists, mbFailedArtists);
    }
    
    private double calculatePopularityScore(Integer popularity, Integer followers) {
        double popularityNorm = (popularity != null && popularity >= 0) ? popularity / 100.0 : 0.0;
        
        double followersNorm = 0.0;
        if (followers != null && followers > 0) {
            double logFollowers = Math.log10(followers);
            followersNorm = Math.max(0.0, Math.min(1.0, (logFollowers - 3.0) / 5.0));
        }
        
        return popularityNorm * 0.6 + followersNorm * 0.4;
    }
    
    private List<ArtistData> filterByWikidataDissolved(List<ArtistData> candidates) {
        List<ArtistData> activeArtists = new ArrayList<>();
        
        for (ArtistData data : candidates) {
            boolean isDissolved = false;
            
            try {
                Optional<String> qidOpt = wikidataClient.searchWikidataIdBySpotifyId(data.spotifyId);
                if (qidOpt.isEmpty()) {
                    activeArtists.add(data);
                    continue;
                }
                
                String qid = qidOpt.get();
                
                Optional<JsonNode> entityOpt = wikidataClient.getEntityInfo(qid);
                if (entityOpt.isEmpty()) {
                    activeArtists.add(data);
                    continue;
                }
                
                JsonNode entity = entityOpt.get();
                
                Optional<String> p576Opt = getTimeClaim(entity, "P576");
                if (p576Opt.isPresent()) {
                    isDissolved = true;
                }
                
                if (!isDissolved) {
                    Optional<String> p582Opt = getTimeClaim(entity, "P582");
                    if (p582Opt.isPresent()) {
                        isDissolved = true;
                    }
                }
                
            } catch (Exception e) {
                // Wikidata 조회 실패는 다음 단계로 진행 (보수적 접근)
            }
            
            if (!isDissolved) {
                activeArtists.add(data);
            }
        }
        
        return activeArtists;
    }
    
    private Optional<String> getTimeClaim(JsonNode entity, String propertyId) {
        try {
            JsonNode claims = entity.path("claims").path(propertyId);
            if (!claims.isArray() || claims.isEmpty()) {
                return Optional.empty();
            }
            
            JsonNode value = claims.get(0)
                    .path("mainsnak")
                    .path("datavalue")
                    .path("value");
            
            JsonNode timeNode = value.path("time");
            if (!timeNode.isMissingNode() && !timeNode.asText().isBlank()) {
                return Optional.of(timeNode.asText());
            }
            
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
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

            Artist dbArtist = artistRepository.findById(artistId)
                    .orElse(null);
            String nameKo = dbArtist != null ? dbArtist.getNameKo() : null;

            Track[] topTracks = safeGetTopTracks(api, spotifyArtistId);
            Paging<AlbumSimplified> albums = safeGetAlbums(api, spotifyArtistId);

            List<RelatedArtistResponse> relatedResponses = getRelatedArtists(
                    artistId,
                    artistGroup,
                    artistType,
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
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("rate limit retry exhausted")) {
                log.error("Spotify 상세 조회 실패 (재시도 모두 실패): artistId={}", spotifyArtistId);
                throw new BusinessException(ArtistErrorCode.SPOTIFY_API_ERROR);
            }
            throw e;
        } catch (Exception e) {
            log.error("Spotify 상세 조회 실패: artistId={}", spotifyArtistId, e);
            throw new BusinessException(ArtistErrorCode.SPOTIFY_API_ERROR);
        }
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

    /**
     * 관련 아티스트 추천 (3단계: Recall -> Score -> Diversity)
     */
    private List<RelatedArtistResponse> getRelatedArtists(
            long artistId,
            String artistGroup,
            ArtistType artistType,
            Long genreId
    ) {
        try {
            // 1단계: 후보 뽑기 (Recall)
            Set<Artist> candidates = collectRelatedCandidates(artistId, artistGroup, artistType, genreId);
            
            if (candidates.isEmpty()) {
                return List.of();
            }
            
            // 2단계: 점수 매기기 (Score)
            List<ScoredArtist> scoredArtists = scoreCandidates(candidates, artistGroup, artistType, genreId, artistId);
            
            // 3단계: 4~5명 뽑기 + 도배 방지 (Diversity)
            List<Artist> selectedArtists = selectWithDiversity(scoredArtists, artistGroup, genreId);
            
            // RelatedArtistResponse로 변환
            return selectedArtists.stream()
                        .map(a -> new RelatedArtistResponse(
                                a.getId(),
                                a.getArtistName(),
                                a.getNameKo(),
                                a.getImageUrl(),
                                a.getSpotifyArtistId()
                        ))
                        .toList();
                    
        } catch (Exception e) {
            log.error("관련 아티스트 조회 실패: artistId={}", artistId, e);
            return List.of();
        }
    }
    
    /**
     * 1단계: 후보 뽑기 (Recall)
     * - 같은 genre인 아티스트들 (최대 200명, 결정론적 다양성을 위해 넓은 후보 풀 확보)
     * - 같은 artistGroup인 아티스트들 (최대 5명, artistGroup이 있을 때만)
     * - 같은 artistType인 아티스트들 (최대 50명, fallback: 장르/그룹 후보가 부족할 때만)
     */
    private Set<Artist> collectRelatedCandidates(
            long artistId,
            String artistGroup,
            ArtistType artistType,
            Long genreId
    ) {
        Set<Artist> candidates = new HashSet<>();
        final int MAX_GENRE_CANDIDATES = 200; // 결정론적 다양성을 위해 후보 풀 확장
        final int MAX_GROUP_CANDIDATES = 5;
        final int MAX_TYPE_CANDIDATES = 50;
        final int MIN_CANDIDATES_FOR_FALLBACK = 10; // 이보다 적으면 타입 후보 추가
        
        // 같은 genre인 아티스트들
        if (genreId != null) {
            List<Artist> sameGenre = artistRepository.findByGenreIdAndIdNot(
                    genreId, artistId, 
                    org.springframework.data.domain.PageRequest.of(0, MAX_GENRE_CANDIDATES)
            );
            candidates.addAll(sameGenre);
        }
        
        // 같은 artistGroup인 아티스트들 (artistGroup이 있을 때만)
        if (artistGroup != null && !artistGroup.isBlank()) {
            List<Artist> sameGroup = artistRepository.findByArtistGroupAndIdNot(
                    artistGroup, artistId,
                    org.springframework.data.domain.PageRequest.of(0, MAX_GROUP_CANDIDATES)
            );
            candidates.addAll(sameGroup);
        }
        
        // 같은 artistType인 아티스트들 (fallback: 후보가 부족할 때만)
        if (artistType != null && candidates.size() < MIN_CANDIDATES_FOR_FALLBACK) {
            List<Artist> sameType = artistRepository.findByArtistTypeAndIdNot(
                    artistType, artistId,
                    org.springframework.data.domain.PageRequest.of(0, MAX_TYPE_CANDIDATES)
            );
            candidates.addAll(sameType);
        }
        
        return candidates;
    }
    
    /**
     * 2단계: 점수 매기기 (Score)
     * - 같은 그룹: +80
     * - 같은 장르: +60 (그룹 점수가 있을 때는 +30으로 완화)
     * - 같은 타입: +15
     * - likeCount 보정: + 5 * log(likeCount+1), 최대 15점 (기본 연관 점수가 30 이상일 때만 적용)
     * - hash 기반 미세 조정: 점수에 직접 반영하여 기준 아티스트별로 다른 결과 보장
     * 
     * 정렬: 점수(내부에 hash 반영) → likeCount → 이름 → Spotify ID → id
     * hash를 점수에 직접 반영하여 같은 점수/likeCount를 가진 아티스트들도 기준 아티스트별로 다른 순서를 보장
     */
    private List<ScoredArtist> scoreCandidates(
            Set<Artist> candidates,
            String artistGroup,
            ArtistType artistType,
            Long genreId,
            long baseArtistId  // 기준 아티스트 ID (hash 계산용)
    ) {
        List<ScoredArtist> scored = new ArrayList<>();
        final double MAX_LIKECOUNT_BONUS = 15.0;
        final double MIN_BASE_SCORE_FOR_LIKECOUNT = 30.0; // 기본 연관 점수가 이 이상일 때만 likeCount 보정 적용
        
        for (Artist candidate : candidates) {
            double score = 0.0;
            boolean hasGroupScore = false;
            
            // 같은 그룹이면 +80
            if (artistGroup != null && !artistGroup.isBlank() && 
                candidate.getArtistGroup() != null && 
                candidate.getArtistGroup().equals(artistGroup)) {
                score += 80;
                hasGroupScore = true;
            }
            
            // 같은 장르면 +60 (그룹 점수가 있을 때는 +30으로 완화하여 중복 가산 완화)
            if (genreId != null) {
                boolean hasSameGenre = candidate.getArtistGenres().stream()
                        .anyMatch(ag -> ag.getGenre().getId() == genreId);
                if (hasSameGenre) {
                    // 그룹 점수가 있으면 장르 점수를 절반으로 완화
                    score += hasGroupScore ? 30 : 60;
                }
            }
            
            // 같은 타입이면 +15
            if (artistType != null && candidate.getArtistType() == artistType) {
                score += 15;
            }
            
            // likeCount 보정: 기본 연관 점수가 일정 수준 이상일 때만 적용, 최대 15점
            double baseScore = score; // likeCount 보정 전 점수
            if (baseScore >= MIN_BASE_SCORE_FOR_LIKECOUNT && candidate.getLikeCount() > 0) {
                double likeCountBonus = 5.0 * Math.log(candidate.getLikeCount() + 1);
                score += Math.min(likeCountBonus, MAX_LIKECOUNT_BONUS);
            }
            
            // hash 기반 tie-breaker 값 계산 (기준 아티스트 ID와 후보 아티스트 ID 조합)
            int hashValue = calculateHashForTieBreaker(baseArtistId, candidate.getId());
            
            // hash를 점수에 반영하여 기준 아티스트별로 다른 순서 보장
            // hashValue를 0~1 범위로 정규화하여 점수에 더함 (최대 약 1점 차이)
            // 음수 hash도 처리하기 위해 절댓값 사용 후 modulo 연산
            double normalizedHash = (Math.abs(hashValue) % 10000) / 10000.0; // 0.0 ~ 0.9999
            score += normalizedHash; // 최대 약 1점 차이로 같은 점수/likeCount를 가진 아티스트들도 순서가 달라짐
            
            scored.add(new ScoredArtist(candidate, score, hashValue));
        }
        
        // 점수 내림차순 정렬, 동점일 때는 의미 있는 기준으로 정렬
        scored.sort((a, b) -> {
            // 1순위: 점수 내림차순 (이미 hash가 반영되어 있음)
            int scoreCompare = Double.compare(b.score, a.score);
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            
            // 2순위: likeCount 내림차순
            int likeCountCompare = Integer.compare(b.artist.getLikeCount(), a.artist.getLikeCount());
            if (likeCountCompare != 0) {
                return likeCountCompare;
            }
            
            // 3순위: 이름 오름차순 (nameKo 우선, 없으면 artistName)
            String nameA = a.artist.getNameKo() != null && !a.artist.getNameKo().isBlank() 
                    ? a.artist.getNameKo() 
                    : a.artist.getArtistName();
            String nameB = b.artist.getNameKo() != null && !b.artist.getNameKo().isBlank() 
                    ? b.artist.getNameKo() 
                    : b.artist.getArtistName();
            int nameCompare = nameA.compareTo(nameB);
            if (nameCompare != 0) {
                return nameCompare;
            }
            
            // 4순위: Spotify ID 오름차순
            if (a.artist.getSpotifyArtistId() != null && b.artist.getSpotifyArtistId() != null) {
                int spotifyIdCompare = a.artist.getSpotifyArtistId().compareTo(b.artist.getSpotifyArtistId());
                if (spotifyIdCompare != 0) {
                    return spotifyIdCompare;
                }
            }
            
            // 최종 tie-breaker: id 오름차순
            return Long.compare(a.artist.getId(), b.artist.getId());
        });
        
        return scored;
    }
    
    /**
     * 기준 아티스트 ID와 후보 아티스트 ID를 조합하여 hash 값 계산
     * 
     * 같은 기준 아티스트에 대해서는 항상 동일한 hash 값을 반환하지만,
     * 기준 아티스트가 다르면 같은 후보라도 다른 hash 값을 가져 결정론적 다양성을 보장합니다.
     * 
     * @param baseArtistId 기준 아티스트 ID
     * @param candidateArtistId 후보 아티스트 ID
     * @return hash 값 (정렬용)
     */
    private int calculateHashForTieBreaker(long baseArtistId, long candidateArtistId) {
        // 두 ID를 조합하여 hash 계산
        String combined = baseArtistId + "-" + candidateArtistId;
        return combined.hashCode();
    }
    
    /**
     * 3단계: 슬롯 기반 최종 선택 (Diversity)
     * 
     * 슬롯 구조로 구성 비율을 강제하여 장르 편향을 완화합니다.
     * 랜덤 요소 없이 점수 순으로 고정적으로 선별하므로, 동일한 아티스트 조회 시 항상 동일한 결과를 보장합니다.
     * 
     * 슬롯 구성:
     * - 그룹 슬롯: 같은 그룹 최대 2명
     * - 장르 슬롯: 같은 장르(그룹 아님) 최대 3명
     * - 그 외 슬롯: 나머지
     * 
     * 목표: 최대 5명
     */
    private List<Artist> selectWithDiversity(
            List<ScoredArtist> scoredArtists,
            String artistGroup,
            Long genreId
    ) {
        final int MAX_SAME_GROUP = 2;
        final int MAX_SAME_GENRE = 3;
        final int TARGET_COUNT = 5;
        
        // 슬롯별로 후보 분류
        List<ScoredArtist> groupSlot = new ArrayList<>();
        List<ScoredArtist> genreSlot = new ArrayList<>();
        List<ScoredArtist> otherSlot = new ArrayList<>();
        
        for (ScoredArtist scored : scoredArtists) {
            Artist candidate = scored.artist;
            
            // 같은 그룹 체크
            boolean isSameGroup = artistGroup != null && !artistGroup.isBlank() &&
                    candidate.getArtistGroup() != null &&
                    candidate.getArtistGroup().equals(artistGroup);
            
            // 같은 장르 체크
            boolean isSameGenre = genreId != null && candidate.getArtistGenres().stream()
                    .anyMatch(ag -> ag.getGenre().getId() == genreId);
            
            if (isSameGroup) {
                groupSlot.add(scored);
            } else if (isSameGenre) {
                genreSlot.add(scored);
            } else {
                otherSlot.add(scored);
            }
        }
        
        // 슬롯별로 최종 선택 (각 슬롯 내에서는 이미 점수 순으로 정렬되어 있음)
        List<Artist> selected = new ArrayList<>();
        
        // 1. 그룹 슬롯에서 최대 2명 선택
        for (int i = 0; i < Math.min(MAX_SAME_GROUP, groupSlot.size()) && selected.size() < TARGET_COUNT; i++) {
            selected.add(groupSlot.get(i).artist);
        }
        
        // 2. 장르 슬롯에서 최대 3명 선택
        for (int i = 0; i < Math.min(MAX_SAME_GENRE, genreSlot.size()) && selected.size() < TARGET_COUNT; i++) {
            selected.add(genreSlot.get(i).artist);
        }
        
        // 3. 그 외 슬롯에서 나머지 채우기
        for (ScoredArtist scored : otherSlot) {
            if (selected.size() >= TARGET_COUNT) {
                break;
            }
            selected.add(scored.artist);
        }
        
        return selected;
    }
    
    /**
     * 점수가 매겨진 아티스트
     */
    private static class ScoredArtist {
        final Artist artist;
        final double score;
        final int hashValue; // hash 기반 tie-breaker 값
        
        ScoredArtist(Artist artist, double score) {
            this.artist = artist;
            this.score = score;
            this.hashValue = 0; // 하위 호환성
        }
        
        ScoredArtist(Artist artist, double score, int hashValue) {
            this.artist = artist;
            this.score = score;
            this.hashValue = hashValue;
        }
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
