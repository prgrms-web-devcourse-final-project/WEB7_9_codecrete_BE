package com.back.web7_9_codecrete_be.domain.concerts.repository;

import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import com.back.web7_9_codecrete_be.domain.concerts.entity.ConcertLike;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConcertLikeRepository extends JpaRepository<ConcertLike, Long> {
    ConcertLike findConcertLikeByConcertAndUser(Concert concert, User user);
}

