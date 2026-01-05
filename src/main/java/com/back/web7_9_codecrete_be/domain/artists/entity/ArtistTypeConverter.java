package com.back.web7_9_codecrete_be.domain.artists.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Converter(autoApply = true)
public class ArtistTypeConverter implements AttributeConverter<ArtistType, String> {

    @Override
    public String convertToDatabaseColumn(ArtistType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public ArtistType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        
        // 숫자로 저장된 잘못된 값 처리 (예: "0", "1", "2")
        if (dbData.matches("\\d+")) {
            log.warn("잘못된 artist_type 값 발견 (숫자): {}, null로 처리", dbData);
            return null;
        }
        
        try {
            return ArtistType.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 artist_type 값 발견: {}, null로 처리", dbData);
            return null;
        }
    }
}


