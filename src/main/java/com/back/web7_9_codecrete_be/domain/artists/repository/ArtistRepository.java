package com.back.web7_9_codecrete_be.domain.artists.repository;

import com.back.web7_9_codecrete_be.domain.artists.entity.Artist;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, Long> {
    boolean existsBySpotifyArtistId(String spotifyArtistId);
    java.util.Optional<Artist> findBySpotifyArtistId(String spotifyArtistId);
    
    @Query("SELECT a FROM Artist a WHERE a.nameKo IS NULL ORDER BY a.id ASC")
    List<Artist> findByNameKoIsNullOrderByIdAsc(Pageable pageable);

    @Query("SELECT a FROM Artist a WHERE a.musicBrainzId IS NULL ORDER BY a.id ASC")
    List<Artist> findByMusicBrainzIdIsNullOrderByIdAsc(Pageable pageable);

    boolean existsByArtistName(String artistName);
    boolean existsByNameKo(String nameKo);

    List<Artist> findTop5ByArtistGroupAndIdNot(String artistGroup, long excludeId);
    
    @Query("""
        SELECT DISTINCT a FROM Artist a
        JOIN a.artistGenres ag
        WHERE ag.genre.id = :genreId AND a.id != :excludeId
        ORDER BY a.likeCount DESC
    """)
    List<Artist> findTop5ByGenreIdAndIdNot(@org.springframework.data.repository.query.Param("genreId") Long genreId, 
                                             @org.springframework.data.repository.query.Param("excludeId") long excludeId,
                                             Pageable pageable);

    List<Artist> findAllByArtistNameContainingIgnoreCaseOrNameKoContainingIgnoreCase(String artistName1, String artistName2);

    Slice<Artist> findAllBy(Pageable pageable);

    // 이름순 정렬 (nameKo 우선, 없으면 artistName) - 가나다순
    @Query("""
        SELECT a FROM Artist a 
        ORDER BY 
        CASE 
            WHEN a.nameKo IS NOT NULL AND a.nameKo != '' THEN a.nameKo 
            ELSE a.artistName 
        END ASC
    """)
    Slice<Artist> findAllOrderByName(Pageable pageable);

    // 인기순 정렬 (좋아요 많은 순)
    @Query("SELECT a FROM Artist a ORDER BY a.likeCount DESC")
    Slice<Artist> findAllOrderByLikeCountDesc(Pageable pageable);
}
