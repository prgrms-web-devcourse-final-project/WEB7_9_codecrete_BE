package com.back.web7_9_codecrete_be.domain.artists.repository;

import com.back.web7_9_codecrete_be.domain.artists.entity.Artist;
import com.back.web7_9_codecrete_be.domain.artists.entity.ArtistType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, Long> {
    
    @Query("SELECT a FROM Artist a WHERE a.nameKo IS NULL ORDER BY a.id ASC")
    List<Artist> findByNameKoIsNullOrderByIdAsc(Pageable pageable);

    @Query("SELECT a FROM Artist a WHERE a.musicBrainzId IS NULL ORDER BY a.id ASC")
    List<Artist> findByMusicBrainzIdIsNullOrderByIdAsc(Pageable pageable);

    boolean existsByArtistName(String artistName);
    boolean existsByNameKo(String nameKo);

    // 같은 artistGroup인 아티스트들 조회 (관련 아티스트 추천용) - artistGenres를 fetch join하여 N+1 문제 방지
    @Query("""
        SELECT DISTINCT a FROM Artist a
        LEFT JOIN FETCH a.artistGenres ag
        LEFT JOIN FETCH ag.genre
        WHERE a.artistGroup = :artistGroup AND a.id != :excludeId
        ORDER BY a.likeCount DESC, a.id ASC
    """)
    List<Artist> findByArtistGroupAndIdNot(@Param("artistGroup") String artistGroup,
                                           @Param("excludeId") long excludeId,
                                           Pageable pageable);
    
    // 같은 genre인 아티스트들 조회 (관련 아티스트 추천용) - artistGenres와 genre를 fetch join하여 N+1 문제 방지
    @Query("""
        SELECT DISTINCT a FROM Artist a
        JOIN FETCH a.artistGenres ag
        JOIN FETCH ag.genre
        WHERE ag.genre.id = :genreId AND a.id != :excludeId
        ORDER BY a.likeCount DESC, a.id ASC
    """)
    List<Artist> findByGenreIdAndIdNot(@Param("genreId") Long genreId, 
                                        @Param("excludeId") long excludeId,
                                        Pageable pageable);

    // 장르별 아티스트 목록 조회 - artistGenres와 genre를 fetch join하여 N+1 문제 방지
    @Query("""
        SELECT DISTINCT a FROM Artist a
        JOIN FETCH a.artistGenres ag
        JOIN FETCH ag.genre g
        WHERE g.id = :genreId
        ORDER BY a.likeCount DESC, a.id ASC
    """)
    List<Artist> findArtistsByGenreId(@Param("genreId") Long genreId);

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

    // 배치 조회: spotifyId 리스트로 존재하는 아티스트의 spotifyId만 반환
    @Query("SELECT a.spotifyArtistId FROM Artist a WHERE a.spotifyArtistId IN :spotifyIds")
    List<String> findSpotifyIdsBySpotifyIdsIn(@Param("spotifyIds") List<String> spotifyIds);
    
    // 배치 조회: spotifyId 리스트로 존재하는 아티스트 전체 엔티티 반환 (Bulk 저장용)
    @Query("SELECT a FROM Artist a WHERE a.spotifyArtistId IN :spotifyIds")
    List<Artist> findBySpotifyArtistIdIn(@Param("spotifyIds") List<String> spotifyIds);
    
    // 같은 artistType인 아티스트들 조회 (관련 아티스트 추천용, fallback) - artistGenres를 fetch join하여 N+1 문제 방지
    @Query("""
        SELECT DISTINCT a FROM Artist a
        LEFT JOIN FETCH a.artistGenres ag
        LEFT JOIN FETCH ag.genre
        WHERE a.artistType = :artistType AND a.id != :excludeId
        ORDER BY a.likeCount DESC, a.id ASC
    """)
    List<Artist> findByArtistTypeAndIdNot(@Param("artistType") ArtistType artistType,
                                          @Param("excludeId") long excludeId,
                                          Pageable pageable);
}
