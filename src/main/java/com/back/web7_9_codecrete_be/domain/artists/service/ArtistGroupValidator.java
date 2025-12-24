package com.back.web7_9_codecrete_be.domain.artists.service;

import org.springframework.stereotype.Component;

@Component
public class ArtistGroupValidator {

    /**
     * artistGroup 검증: 멤버 이름, 출연 프로그램, 소속사 등 잘못된 값 필터링
     */
    public String validate(String groupName, String artistName, String nameKo) {
        if (groupName == null || groupName.isBlank()) {
            return null;
        }
        
        String normalizedGroupName = normalizeForComparison(groupName);
        String lowerGroupName = groupName.toLowerCase().trim();
        
        if (normalizedGroupName.length() <= 3) {
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
        
        String[] programKeywords = {
            "produce", "show", "survival", "audition", "competition",
            "프로듀스", "쇼", "서바이벌", "오디션", "경쟁", "프로그램"
        };
        for (String keyword : programKeywords) {
            if (lowerGroupName.contains(keyword)) {
                return null;
            }
        }
        
        String[] companyKeywords = {
            "sm entertainment", "yg entertainment", "jyp entertainment", "cube entertainment",
            "pledis entertainment", "starship entertainment", "fantagio", "woollim",
            "fnc entertainment", "rbw", "source music", "bighit", "hybe",
            "sm", "yg", "jyp", "cube", "pledis", "starship", "fantagio",
            "woollim", "fnc", "source", "bighit", "hybe",
            "엔터테인먼트", "엔터", "기획사", "소속사"
        };
        for (String keyword : companyKeywords) {
            if (lowerGroupName.contains(keyword)) {
                return null;
            }
        }
        
        return groupName;
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
