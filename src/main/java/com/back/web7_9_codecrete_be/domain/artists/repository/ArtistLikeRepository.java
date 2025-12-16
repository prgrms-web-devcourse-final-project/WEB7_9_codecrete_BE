package com.back.web7_9_codecrete_be.domain.artists.repository;

import com.back.web7_9_codecrete_be.domain.artists.entity.Artist;
import com.back.web7_9_codecrete_be.domain.artists.entity.ArtistLike;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtistLikeRepository extends JpaRepository<ArtistLike, Long> {
    long countByArtistId(Long artistId);
    boolean existsByArtistAndUser(Artist artist, User user);
}
