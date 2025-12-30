package com.back.web7_9_codecrete_be.domain.community.post.repository;

import com.back.web7_9_codecrete_be.domain.community.post.entity.JoinPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JoinPostRepository extends JpaRepository<JoinPost, Long> {
    List<JoinPost> findByConcertId(Long concertId);
}
