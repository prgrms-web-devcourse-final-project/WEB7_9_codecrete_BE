package com.back.web7_9_codecrete_be.domain.community.post.repository;

import com.back.web7_9_codecrete_be.domain.community.post.entity.JoinPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JoinPostRepository extends JpaRepository<JoinPost, Long> {
    Page<JoinPost> findByPost_UserId(Long userId, Pageable pageable);
}
