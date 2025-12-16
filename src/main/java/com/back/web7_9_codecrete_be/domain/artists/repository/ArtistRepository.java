package com.back.web7_9_codecrete_be.domain.artists.repository;

import com.back.web7_9_codecrete_be.domain.artists.entity.Artist;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, Long> {
    boolean existsBySpotifyArtistId(String spotifyArtistId);
    
    @Query("SELECT a FROM Artist a WHERE a.nameKo IS NULL ORDER BY a.id ASC")
    List<Artist> findByNameKoIsNullOrderByIdAsc(Pageable pageable);

    boolean existsByArtistName(String artistName);
    boolean existsByNameKo(String nameKo);

    List<Artist> findTop5ByArtistGroupAndIdNot(String artistGroup, long excludeId);
    List<Artist> findTop5ByGenreIdAndIdNot(Long genreId, long excludeId);
}
