package com.back.web7_9_codecrete_be.domain.artists.service.artistEnrichService;

import org.springframework.stereotype.Component;

@Component
public class ArtistGroupValidator {

    /**
     * 소스 신뢰도 레벨
     */
    public enum SourceTrustLevel {
        HIGH,    // Wikidata, MusicBrainz - 신뢰도 높음
        LOW      // FLO 등 - 신뢰도 낮음
    }

    /**
     * artistGroup 검증: 멤버 이름, 출연 프로그램, 소속사 등 잘못된 값 필터링
     * 
     * @param groupName 검증할 그룹명
     * @param artistName 아티스트 이름 (중복 체크용)
     * @param nameKo 아티스트 한국어 이름 (중복 체크용)
     * @param trustLevel 소스 신뢰도 (HIGH: Wikidata/MB는 3글자 허용, LOW: FLO는 3글자 차단)
     */
    public String validate(String groupName, String artistName, String nameKo, SourceTrustLevel trustLevel) {
        if (groupName == null || groupName.isBlank()) {
            return null;
        }
        
        String normalizedGroupName = normalizeForComparison(groupName);
        String lowerGroupName = groupName.toLowerCase().trim();
        
        // 신뢰도 낮은 소스(FLO)에서만 3글자 이하 차단, 신뢰도 높은 소스(Wikidata/MB)는 허용
        if (trustLevel == SourceTrustLevel.LOW && normalizedGroupName.length() <= 3) {
            return null;
        }
        // 신뢰도 높은 소스는 2글자 이하만 차단 (EXO, BTS 같은 3글자 그룹 허용)
        if (trustLevel == SourceTrustLevel.HIGH && normalizedGroupName.length() <= 2) {
            return null;
        }
        
        if (artistName != null && !artistName.isBlank()) {
            String normalizedArtistName = normalizeForComparison(artistName);
            String lowerArtistName = artistName.toLowerCase().trim();
            
            if (normalizedGroupName.equals(normalizedArtistName) || 
                lowerGroupName.equals(lowerArtistName)) {
                return null;
            }
            
            if (normalizedGroupName.contains(normalizedArtistName) && 
                normalizedArtistName.length() >= 2) {
                if (normalizedGroupName.startsWith(normalizedArtistName) || 
                    normalizedGroupName.endsWith(normalizedArtistName)) {
                    return null;
                }
            }
            
            if (normalizedArtistName.contains(normalizedGroupName) && 
                normalizedGroupName.length() >= 2) {
                return null;
            }
        }
        
        if (nameKo != null && !nameKo.isBlank()) {
            String normalizedNameKo = normalizeForComparison(nameKo);
            String lowerNameKo = nameKo.toLowerCase().trim();
            
            if (normalizedGroupName.equals(normalizedNameKo) || 
                lowerGroupName.equals(lowerNameKo)) {
                return null;
            }
            
            if (normalizedGroupName.contains(normalizedNameKo) && 
                normalizedNameKo.length() >= 2) {
                if (normalizedGroupName.startsWith(normalizedNameKo) || 
                    normalizedGroupName.endsWith(normalizedNameKo)) {
                    return null;
                }
            }
            
            if (normalizedNameKo.contains(normalizedGroupName) && 
                normalizedGroupName.length() >= 2) {
                return null;
            }
        }
        
        // 출연 프로그램 키워드
        String[] programKeywords = {
            "produce", "show", "survival", "audition", "competition", "project",
            "queendom", "kingdom", "girls planet", "boys planet", "planet",
            "show me the money", "k-pop star", "kpop star", "superstar k",
            "the voice", "masked singer", "king of masked singer",
            "sugar man", "immortal songs", "fantastic duo", "hidden singer",
            "i can see your voice", "unpretty rapstar", "good girl",
            "music bank", "music core", "inkigayo", "show champion", "m countdown",
            "the show", "music show", "award", "festival", "concert", "special",
            "프로듀스", "쇼", "서바이벌", "오디션", "경쟁", "프로그램", "프로젝트",
            "퀸덤", "킹덤", "걸스플래닛", "보이즈플래닛", "플래닛",
            "쇼미더머니", "케이팝스타", "슈퍼스타K", "더보이스", "복면가왕",
            "슈가맨", "불후의명곡", "판타스틱듀오", "히든싱어", "너의목소리가보여",
            "언프리티랩스타", "언프리티 랩스타", "굿걸", "뮤직뱅크", "뮤직코어", "인기가요",
            "쇼챔피언", "엠카운트다운", "더쇼", "뮤직쇼", "시상식", "페스티벌", "콘서트"
        };
        for (String keyword : programKeywords) {
            if (lowerGroupName.contains(keyword)) {
                return null;
            }
        }
        
        // 소속사/레이블 키워드
        String[] companyKeywords = {
            "sm entertainment", "yg entertainment", "jyp entertainment", "cube entertainment",
            "pledis entertainment", "starship entertainment", "fantagio", "woollim",
            "fnc entertainment", "rbw", "source music", "bighit", "hybe",
            "wakeone", "cj enm", "mnet", "kbs", "mbc", "sbs",
            "loen entertainment", "loen", "kakao m", "genie music", "genie",
            "melon", "bugs", "flo", "vibe", "smtown",
            "sm", "yg", "jyp", "cube", "pledis", "starship", "fantagio",
            "woollim", "fnc", "source", "bighit", "hybe", "wakeone",
            "엔터테인먼트", "엔터", "기획사", "소속사", "레이블", "스튜디오"
        };
        for (String keyword : companyKeywords) {
            if (lowerGroupName.contains(keyword)) {
                return null;
            }
        }
        
        // 이벤트성/일회성 그룹 키워드
        String[] eventKeywords = {
            "collaboration", "collab", "special stage", "special unit",
            "project group", "temporary", "one-time", "event",
            "collaboration stage", "collab stage", "special collaboration",
            "합동", "콜라보", "스페셜", "특별", "임시", "일회성", "이벤트",
            "프로젝트 그룹", "특별 무대", "합동 무대", "콜라보 무대"
        };
        for (String keyword : eventKeywords) {
            if (lowerGroupName.contains(keyword)) {
                return null;
            }
        }
        
        // 숫자만 있는 경우 (예: "101", "2020" 등)
        if (normalizedGroupName.matches("^\\d+$")) {
            return null;
        }
        
        return groupName;
    }
    
    /**
     * artistGroup 검증 (하위 호환성, 기본값 LOW)
     * 
     * @deprecated 소스 신뢰도를 명시하는 validate(String, String, String, SourceTrustLevel) 사용 권장
     */
    @Deprecated
    public String validate(String groupName, String artistName, String nameKo) {
        return validate(groupName, artistName, nameKo, SourceTrustLevel.LOW);
    }
    
    /**
     * 이름 정규화 (비교용)
     */
    private String normalizeForComparison(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase()
                .replaceAll("[\\s\\-_\\(\\)\\[\\]]", "")
                .trim();
    }
}
