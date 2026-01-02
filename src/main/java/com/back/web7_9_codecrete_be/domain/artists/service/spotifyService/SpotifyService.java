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
    
    // 아티스트 시드 설정
    private static final int SEARCH_LIMIT = 50;
    private static final int TARGET_CANDIDATES = 1200; // Phase 1 후보 풀 목표 수 (800~1500)
    private static final int MAX_PAGES_PER_KEYWORD = 2; // 키워드당 최대 페이지 수 (429 완화)
    private static final int MAX_SEED_COUNT = 300; // 최종 저장할 아티스트 수
    private static final int API_CALL_DELAY_MS = 200; // API 호출 간 대기 시간
    
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

    // Phase 1: 넓은 후보 풀 수집 (키워드당 최대 2페이지)
    private List<ArtistData> collectArtistsRoundRobin(SpotifyApi api, Map<String, CategoryConfig> categoryConfigs) {
        List<ArtistData> artistDataList = new ArrayList<>();
        Set<String> collectedSpotifyIds = new HashSet<>();
        
        Map<String, Integer> categoryKeywordIndex = initializeCategoryTracking(categoryConfigs);
        Map<String, Map<String, Integer>> categoryKeywordPageIndex = initializeCategoryPageTracking(categoryConfigs);
        
        // 모든 키워드를 순회하며 최대 2페이지씩 수집
        boolean shouldContinue = true;
        while (shouldContinue && artistDataList.size() < TARGET_CANDIDATES) {
            boolean anyProgress = false;
            
            for (Map.Entry<String, CategoryConfig> entry : categoryConfigs.entrySet()) {
                String categoryName = entry.getKey();
                CategoryConfig config = entry.getValue();
                
                if (artistDataList.size() >= TARGET_CANDIDATES) {
                    shouldContinue = false;
                    break;
                }
                
                int keywordIndex = categoryKeywordIndex.get(categoryName);
                if (keywordIndex >= config.keywords.size()) {
                    continue;
                }
                
                String query = config.keywords.get(keywordIndex);
                Map<String, Integer> keywordPageIndex = categoryKeywordPageIndex.get(categoryName);
                int currentPage = keywordPageIndex.getOrDefault(query, 0);
                
                // 키워드당 최대 2페이지까지만 수집
                if (currentPage >= MAX_PAGES_PER_KEYWORD) {
                    categoryKeywordIndex.put(categoryName, keywordIndex + 1);
                    continue;
                }
                
                try {
                    boolean progress = processSearchPage(api, query, currentPage, config, categoryName,
                            artistDataList, collectedSpotifyIds, categoryKeywordIndex,
                            categoryKeywordPageIndex, keywordPageIndex);
                    if (progress) {
                        anyProgress = true;
                    }
                } catch (Exception e) {
                    categoryKeywordIndex.put(categoryName, keywordIndex + 1);
                }
            }
            
            if (!anyProgress) {
                break;
            }
        }
        
        if (artistDataList.isEmpty()) {
            throw new BusinessException(ArtistErrorCode.ARTIST_SEED_FAILED);
        }
        
        // 배치로 DB 중복 제거
        return removeExistingArtists(artistDataList);
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

    private boolean processSearchPage(SpotifyApi api, String query, int currentPage, CategoryConfig config,
                                     String categoryName, List<ArtistData> artistDataList,
                                     Set<String> collectedSpotifyIds, Map<String, Integer> categoryKeywordIndex,
                                     Map<String, Map<String, Integer>> categoryKeywordPageIndex,
                                     Map<String, Integer> keywordPageIndex) throws Exception {
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
            return false;
        }
        
        boolean anyProgress = false;

        for (var spotifyArtist : items) {
            if (artistDataList.size() >= TARGET_CANDIDATES) {
                break;
            }
            
            if (!shouldAddArtist(spotifyArtist, config, categoryName, collectedSpotifyIds)) {
                continue;
            }
            
            ArtistData artistData = createArtistData(spotifyArtist);
            artistDataList.add(artistData);
            collectedSpotifyIds.add(artistData.spotifyId);
            anyProgress = true;
        }
        
        keywordPageIndex.put(query, currentPage + 1);
        if (items.length < SEARCH_LIMIT || offset + SEARCH_LIMIT >= paging.getTotal()) {
            categoryKeywordIndex.put(categoryName, categoryKeywordIndex.get(categoryName) + 1);
        }
        
        Thread.sleep(API_CALL_DELAY_MS);
        
        return anyProgress;
    }

    // 카테고리별 필터 임계값 적용 (popularity + followers AND 조합)
    private boolean shouldAddArtist(se.michaelthelin.spotify.model_objects.specification.Artist spotifyArtist,
                                   CategoryConfig config, String categoryName, Set<String> collectedSpotifyIds) {
        String spotifyId = spotifyArtist.getId();
        String name = spotifyArtist.getName();
        
        if (spotifyId == null || name == null || name.isBlank() || collectedSpotifyIds.contains(spotifyId)) {
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
        
        // 카테고리별 필터 임계값 적용
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

    private List<ArtistData> applyFilteringPipeline(List<ArtistData> artistDataList) {
        List<ArtistData> filteredByDb = filterByDbInfo(artistDataList);
        List<ArtistData> filteredByUnit = filterUnitExclude(filteredByDb);
        MusicBrainzFilterResult mbResult = filterByMusicBrainzEnded(filteredByUnit);
        List<ArtistData> filteredByWikidata = filterByWikidataDissolved(mbResult.getMbFailedArtists());
        
        List<ArtistData> afterFiltering = new ArrayList<>(mbResult.getActiveArtists());
        afterFiltering.addAll(filteredByWikidata);
        
        return selectTopArtistsByScore(afterFiltering);
    }
    
    // 카테고리별 스코어 기반 상위 N명 선발
    private List<ArtistData> selectTopArtistsByScore(List<ArtistData> candidates) {
        if (candidates.isEmpty()) {
            return candidates;
        }
        
        // 카테고리별로 그룹화
        Map<String, List<ArtistData>> byCategory = candidates.stream()
                .collect(Collectors.groupingBy(data -> inferCategoryFromGenres(data.genres)));
        
        List<ArtistData> selected = new ArrayList<>();
        Map<String, CategoryConfig> categoryConfigs = createCategoryConfigs();
        
        // 각 카테고리에서 스코어 기반 상위 N명 선발
        for (Map.Entry<String, List<ArtistData>> entry : byCategory.entrySet()) {
            String categoryName = entry.getKey();
            List<ArtistData> categoryCandidates = entry.getValue();
            CategoryConfig config = categoryConfigs.get(categoryName);
            
            if (config == null) {
                continue;
            }
            
            int targetCount = config.targetCount;
            
            List<ArtistData> topInCategory = categoryCandidates.stream()
                    .sorted((a1, a2) -> {
                        double score1 = calculateConcertScore(a1.popularity, a1.followers);
                        double score2 = calculateConcertScore(a2.popularity, a2.followers);
                        return Double.compare(score2, score1);
                    })
                    .limit(targetCount)
                    .collect(toList());
            
            selected.addAll(topInCategory);
        }
        
        // 카테고리별로 선발된 총합이 300명을 넘으면 전체 스코어로 재정렬
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
    
    private Map<String, Artist> upsertArtists(List<ArtistData> finalArtists) {
        Map<String, Artist> artistMap = new HashMap<>();
        
        for (ArtistData data : finalArtists) {
            Optional<Artist> existingArtistOpt = artistRepository.findBySpotifyArtistId(data.spotifyId);
            
            Artist artist;
            if (existingArtistOpt.isPresent()) {
                artist = existingArtistOpt.get();
                artist.setArtistName(data.name);
                artist.setArtistType(data.artistType);
                artist.setImageUrl(data.imageUrl);
                artist.getArtistGenres().clear();
            } else {
                artist = new Artist(data.spotifyId, data.name, null, data.artistType);
                artist.setImageUrl(data.imageUrl);
            }
            
            artistRepository.save(artist);
            artistMap.put(data.spotifyId, artist);
        }

        return artistMap;
    }
    
    private Map<String, Genre> processGenres(List<ArtistData> finalArtists) {
        Set<String> allGenreNames = finalArtists.stream()
                    .flatMap(data -> data.genres.stream())
                    .filter(Objects::nonNull)
                    .filter(g -> !g.isBlank())
                    .collect(Collectors.toSet());

            if (allGenreNames.isEmpty()) {
                return new HashMap<>();
            }

            List<Genre> existingGenres = genreRepository.findByGenreNameIn(new ArrayList<>(allGenreNames));
            Set<String> existingGenreNames = existingGenres.stream()
                    .map(Genre::getGenreName)
                    .collect(Collectors.toSet());

            List<String> newGenreNames = allGenreNames.stream()
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

    private int createArtistGenreMappings(List<ArtistData> finalArtists, Map<String, Artist> artistMap,
                                         Map<String, Genre> genreMap) {
            int totalMappings = 0;
        
        for (ArtistData data : finalArtists) {
                Artist artist = artistMap.get(data.spotifyId);
                if (artist == null) {
                    continue;
                }

                for (String genreName : data.genres) {
                if (genreName == null || genreName.isBlank()) {
                    continue;
                }
                    
                    Genre genre = genreMap.get(genreName);
                    if (genre == null) {
                        continue;
                    }

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

        return totalMappings;
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

    private List<RelatedArtistResponse> safeGetRelated(
            SpotifyApi api,
            se.michaelthelin.spotify.model_objects.specification.Artist me,
            String artistGroup,
            long artistId,
            Long genreId
    ) {
        String id = (me == null || me.getId() == null) ? null : me.getId().trim();
        if (id == null || id.isBlank()) {
            return List.of();
        }

        try {
            se.michaelthelin.spotify.model_objects.specification.Artist[] related = rateLimitHandler.callWithRateLimitRetry(() -> {
                try {
                    spotifyRateLimiter.acquire();
                    return api.getArtistsRelatedArtists(id).build().execute();
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException("Exception during getArtistsRelatedArtists API call", e);
                }
            }, "safeGetRelated artistId=" + id);
            
            if (related != null && related.length > 0) {
                return toRelatedArtistResponses(related);
            }

            return fallbackRelatedFromDb(artistGroup, artistId, genreId);

        } catch (RuntimeException e) {
            return fallbackRelatedFromDb(artistGroup, artistId, genreId);
        } catch (Exception e) {
            return fallbackRelatedFromDb(artistGroup, artistId, genreId);
        }
    }

    private List<RelatedArtistResponse> fallbackRelatedFromDb(String artistGroup, long artistId, Long genreId) {
        try {
            if (artistGroup != null && !artistGroup.isBlank()) {
                return artistRepository.findTop5ByArtistGroupAndIdNot(artistGroup, artistId).stream()
                        .map(a -> new RelatedArtistResponse(
                                a.getId(),
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
                                a.getId(),
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

    private List<RelatedArtistResponse> toRelatedArtistResponses(se.michaelthelin.spotify.model_objects.specification.Artist[] artists) {
        if (artists == null) return List.of();
        return Stream.of(artists)
                .filter(Objects::nonNull)
                .map(a -> {
                    Long artistId = null;
                    String nameKo = null;
                    Optional<Artist> dbArtist = artistRepository.findBySpotifyArtistId(a.getId());
                    if (dbArtist.isPresent()) {
                        artistId = dbArtist.get().getId();
                        nameKo = dbArtist.get().getNameKo();
                    }
                    return new RelatedArtistResponse(
                            artistId,
                            a.getName(),
                            nameKo,
                            pickImageUrl(a.getImages()),
                            a.getId()
                    );
                })
                .collect(toList());
    }
}
