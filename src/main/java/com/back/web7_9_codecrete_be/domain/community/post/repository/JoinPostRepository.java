package com.back.web7_9_codecrete_be.domain.community.post.repository;

import com.back.web7_9_codecrete_be.domain.community.post.entity.JoinPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JoinPostRepository extends JpaRepository<JoinPost, Long> {
    Page<JoinPost> findByPost_UserId(Long userId, Pageable pageable);

    @Query("""
    SELECT
        jp
    FROM
        JoinPost jp
    JOIN FETCH
        jp.post p
    WHERE
        p.title LIKE %:keyword%
        OR p.content LIKE %:keyword%
    ORDER BY
        p.createdDate DESC
""")
    List<JoinPost> searchByKeyword(
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
