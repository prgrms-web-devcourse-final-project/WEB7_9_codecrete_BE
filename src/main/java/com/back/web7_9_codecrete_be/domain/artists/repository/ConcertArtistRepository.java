package com.back.web7_9_codecrete_be.domain.artists.repository;

import com.back.web7_9_codecrete_be.domain.artists.entity.ConcertArtist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConcertArtistRepository extends JpaRepository<ConcertArtist, Long> {
    List<ConcertArtist> getConcertArtistsByConcert_ConcertId(Long concertConcertId);
}
