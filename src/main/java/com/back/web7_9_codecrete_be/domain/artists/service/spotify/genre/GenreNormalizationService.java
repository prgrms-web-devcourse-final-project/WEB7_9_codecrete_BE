package com.back.web7_9_codecrete_be.domain.artists.service.spotify.genre;

import org.springframework.stereotype.Service;


@Service
// 장르 정규화 서비스 : Spotify의 다양한 장르명을 통합된 카테고리로 변환
public class GenreNormalizationService {

    // Spotify 장르명을 통합된 카테고리로 정규화
    public String normalizeGenre(String originalGenre) {
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

    // 문자열이 주어진 키워드들 중 하나라도 포함하는지 확인
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}

