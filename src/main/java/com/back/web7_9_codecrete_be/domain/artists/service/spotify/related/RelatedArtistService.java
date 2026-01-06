package com.back.web7_9_codecrete_be.domain.artists.service.spotify.related;

import com.back.web7_9_codecrete_be.domain.artists.dto.response.RelatedArtistResponse;
import com.back.web7_9_codecrete_be.domain.artists.entity.Artist;
import com.back.web7_9_codecrete_be.domain.artists.entity.ArtistType;
import com.back.web7_9_codecrete_be.domain.artists.repository.ArtistRepository;
import com.back.web7_9_codecrete_be.domain.artists.service.spotify.related.model.ScoredArtist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
// 관련 아티스트 추천 서비스 : 3단계 파이프라인: Recall -> Score -> Diversity
public class RelatedArtistService {

    private final ArtistRepository artistRepository;

    private static final int MAX_GENRE_CANDIDATES = 200; // 결정론적 다양성을 위해 후보 풀 확장
    private static final int MAX_GROUP_CANDIDATES = 5;
    private static final int MAX_TYPE_CANDIDATES = 50;
    private static final int MIN_CANDIDATES_FOR_FALLBACK = 10; // 이보다 적으면 타입 후보 추가
    private static final double MAX_LIKECOUNT_BONUS = 15.0;
    private static final double MIN_BASE_SCORE_FOR_LIKECOUNT = 30.0; // 기본 연관 점수가 이 이상일 때만 likeCount 보정 적용
    private static final int MAX_SAME_GROUP = 2;
    private static final int MAX_SAME_GENRE = 3;
    private static final int TARGET_COUNT = 5;

    // 관련 아티스트 추천 (3단계: Recall -> Score -> Diversity)
    public List<RelatedArtistResponse> getRelatedArtists(
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

    // 1단계: 후보 뽑기 (Recall)
    private Set<Artist> collectRelatedCandidates(
            long artistId,
            String artistGroup,
            ArtistType artistType,
            Long genreId
    ) {
        Set<Artist> candidates = new HashSet<>();

        // 같은 genre인 아티스트들
        if (genreId != null) {
            List<Artist> sameGenre = artistRepository.findByGenreIdAndIdNot(
                    genreId, artistId,
                    PageRequest.of(0, MAX_GENRE_CANDIDATES)
            );
            candidates.addAll(sameGenre);
        }

        // 같은 artistGroup인 아티스트들 (artistGroup이 있을 때만)
        if (artistGroup != null && !artistGroup.isBlank()) {
            List<Artist> sameGroup = artistRepository.findByArtistGroupAndIdNot(
                    artistGroup, artistId,
                    PageRequest.of(0, MAX_GROUP_CANDIDATES)
            );
            candidates.addAll(sameGroup);
        }

        // 같은 artistType인 아티스트들 (fallback: 후보가 부족할 때만)
        if (artistType != null && candidates.size() < MIN_CANDIDATES_FOR_FALLBACK) {
            List<Artist> sameType = artistRepository.findByArtistTypeAndIdNot(
                    artistType, artistId,
                    PageRequest.of(0, MAX_TYPE_CANDIDATES)
            );
            candidates.addAll(sameType);
        }

        return candidates;
    }

    // 2단계: 점수 매기기 (Score)
    private List<ScoredArtist> scoreCandidates(
            Set<Artist> candidates,
            String artistGroup,
            ArtistType artistType,
            Long genreId,
            long baseArtistId
    ) {
        List<ScoredArtist> scored = new ArrayList<>();

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

            // 같은 장르면 +60 (그룹 점수가 있을 때는 +30으로 완화)
            if (genreId != null) {
                boolean hasSameGenre = candidate.getArtistGenres().stream()
                        .anyMatch(ag -> ag.getGenre().getId() == genreId);
                if (hasSameGenre) {
                    score += hasGroupScore ? 30 : 60;
                }
            }

            // 같은 타입이면 +15
            if (artistType != null && candidate.getArtistType() == artistType) {
                score += 15;
            }

            // likeCount 보정: 기본 연관 점수가 일정 수준 이상일 때만 적용, 최대 15점
            double baseScore = score;
            if (baseScore >= MIN_BASE_SCORE_FOR_LIKECOUNT && candidate.getLikeCount() > 0) {
                double likeCountBonus = 5.0 * Math.log(candidate.getLikeCount() + 1);
                score += Math.min(likeCountBonus, MAX_LIKECOUNT_BONUS);
            }

            // hash 기반 tie-breaker 값 계산
            int hashValue = calculateHashForTieBreaker(baseArtistId, candidate.getId());

            // hash를 점수에 반영하여 기준 아티스트별로 다른 순서 보장
            double normalizedHash = (Math.abs(hashValue) % 10000) / 10000.0;
            score += normalizedHash;

            scored.add(new ScoredArtist(candidate, score, hashValue));
        }

        // 점수 내림차순 정렬
        scored.sort((a, b) -> {
            int scoreCompare = Double.compare(b.score, a.score);
            if (scoreCompare != 0) {
                return scoreCompare;
            }

            int likeCountCompare = Integer.compare(b.artist.getLikeCount(), a.artist.getLikeCount());
            if (likeCountCompare != 0) {
                return likeCountCompare;
            }

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

            if (a.artist.getSpotifyArtistId() != null && b.artist.getSpotifyArtistId() != null) {
                int spotifyIdCompare = a.artist.getSpotifyArtistId().compareTo(b.artist.getSpotifyArtistId());
                if (spotifyIdCompare != 0) {
                    return spotifyIdCompare;
                }
            }

            return Long.compare(a.artist.getId(), b.artist.getId());
        });

        return scored;
    }

    // 기준 아티스트 ID와 후보 아티스트 ID를 조합하여 hash 값 계산
    private int calculateHashForTieBreaker(long baseArtistId, long candidateArtistId) {
        String combined = baseArtistId + "-" + candidateArtistId;
        return combined.hashCode();
    }

    // 3단계: 슬롯 기반 최종 선택 (Diversity)
    private List<Artist> selectWithDiversity(
            List<ScoredArtist> scoredArtists,
            String artistGroup,
            Long genreId
    ) {
        // 슬롯별로 후보 분류
        List<ScoredArtist> groupSlot = new ArrayList<>();
        List<ScoredArtist> genreSlot = new ArrayList<>();
        List<ScoredArtist> otherSlot = new ArrayList<>();

        for (ScoredArtist scored : scoredArtists) {
            Artist candidate = scored.artist;

            boolean isSameGroup = artistGroup != null && !artistGroup.isBlank() &&
                    candidate.getArtistGroup() != null &&
                    candidate.getArtistGroup().equals(artistGroup);

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

        // 슬롯별로 최종 선택
        List<Artist> selected = new ArrayList<>();

        // 1. 그룹 슬롯에서 최대 2명 선택
        for (int i = 0; i < Math.min(MAX_SAME_GROUP, groupSlot.size()) && selected.size() < TARGET_COUNT; i++) {
            selected.add(groupSlot.get(i).artist);
        }

        // 2. 장르 슬롯에서 선택
        int remainingSlots = TARGET_COUNT - selected.size();
        int genreCount = Math.min(MAX_SAME_GENRE, Math.min(genreSlot.size(), remainingSlots));
        for (int i = 0; i < genreCount && selected.size() < TARGET_COUNT; i++) {
            selected.add(genreSlot.get(i).artist);
        }

        // 3. 그 외 슬롯에서 나머지 채우기
        for (ScoredArtist scored : otherSlot) {
            if (selected.size() >= TARGET_COUNT) {
                break;
            }
            selected.add(scored.artist);
        }

        // 4. 장르 슬롯에서 추가로 채우기
        if (selected.size() < TARGET_COUNT && genreSlot.size() > genreCount) {
            for (int i = genreCount; i < genreSlot.size() && selected.size() < TARGET_COUNT; i++) {
                selected.add(genreSlot.get(i).artist);
            }
        }

        // 5. 그룹 슬롯에서 추가로 채우기
        if (selected.size() < TARGET_COUNT && groupSlot.size() > MAX_SAME_GROUP) {
            for (int i = MAX_SAME_GROUP; i < groupSlot.size() && selected.size() < TARGET_COUNT; i++) {
                selected.add(groupSlot.get(i).artist);
            }
        }

        return selected;
    }
}

