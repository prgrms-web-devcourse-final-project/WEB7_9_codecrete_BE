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

    @Query("SELECT a FROM Artist a WHERE a.artistGroup = :artistGroup AND a.id != :excludeId ORDER BY a.likeCount DESC, a.id ASC")
    List<Artist> findTop5ByArtistGroupAndIdNot(@org.springframework.data.repository.query.Param("artistGroup") String artistGroup,
                                               @org.springframework.data.repository.query.Param("excludeId") long excludeId);
    
    @Query("""
        SELECT DISTINCT a FROM Artist a
        JOIN a.artistGenres ag
        WHERE ag.genre.id = :genreId AND a.id != :excludeId
        ORDER BY a.likeCount DESC, a.id ASC
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

    // 배치 조회: spotifyId 리스트로 존재하는 아티스트의 spotifyId만 반환
    @Query("SELECT a.spotifyArtistId FROM Artist a WHERE a.spotifyArtistId IN :spotifyIds")
    List<String> findSpotifyIdsBySpotifyIdsIn(@org.springframework.data.repository.query.Param("spotifyIds") List<String> spotifyIds);
    
    // 배치 조회: spotifyId 리스트로 존재하는 아티스트 전체 엔티티 반환 (Bulk 저장용)
    @Query("SELECT a FROM Artist a WHERE a.spotifyArtistId IN :spotifyIds")
    List<Artist> findBySpotifyArtistIdIn(@org.springframework.data.repository.query.Param("spotifyIds") List<String> spotifyIds);
    
    // 같은 artistType인 아티스트들 조회 (관련 아티스트 추천용)
    @Query("SELECT a FROM Artist a WHERE a.artistType = :artistType AND a.id != :excludeId ORDER BY a.likeCount DESC, a.id ASC")
    List<Artist> findByArtistTypeAndIdNot(@org.springframework.data.repository.query.Param("artistType") com.back.web7_9_codecrete_be.domain.artists.entity.ArtistType artistType,
                                          @org.springframework.data.repository.query.Param("excludeId") long excludeId,
                                          Pageable pageable);
}
