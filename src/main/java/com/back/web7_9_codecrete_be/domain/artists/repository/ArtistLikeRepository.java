package com.back.web7_9_codecrete_be.domain.artists.repository;

import com.back.web7_9_codecrete_be.domain.artists.entity.Artist;
import com.back.web7_9_codecrete_be.domain.artists.entity.ArtistLike;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArtistLikeRepository extends JpaRepository<ArtistLike, Long> {
    long countByArtistId(Long artistId);
    boolean existsByArtistAndUser(Artist artist, User user);
    Optional<ArtistLike> findByArtistAndUser(Artist artist, User user);
    @Query("""
        select al.artist.id
        from ArtistLike al
        where al.user.id = :userId
    """)
    List<Long> findArtistIdsByUserId(@Param("userId") Long userId);

}
