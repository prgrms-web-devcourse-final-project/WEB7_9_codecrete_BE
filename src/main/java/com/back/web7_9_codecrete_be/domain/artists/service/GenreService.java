package com.back.web7_9_codecrete_be.domain.artists.service;

import com.back.web7_9_codecrete_be.domain.artists.entity.Genre;
import com.back.web7_9_codecrete_be.domain.artists.repository.GenreRepository;
import com.back.web7_9_codecrete_be.global.error.code.GenreErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreRepository genreRepository;

    public Genre findByGenreName(String genreName) {
        String normalized = genreName.trim().toLowerCase();
        Genre genre = genreRepository.findByGenreName(normalized)
                .orElseThrow(() -> new BusinessException(GenreErrorCode.GENRE_NOT_FOUND));
        return genre;
    }
}
