package com.back.web7_9_codecrete_be.domain.artists.repository;

import com.back.web7_9_codecrete_be.domain.artists.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {
    Optional<Genre> findByGenreName(String genreName);
}
