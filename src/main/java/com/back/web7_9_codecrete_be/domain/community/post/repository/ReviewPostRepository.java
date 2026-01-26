package com.back.web7_9_codecrete_be.domain.community.post.repository;

import com.back.web7_9_codecrete_be.domain.community.post.entity.ReviewPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewPostRepository extends JpaRepository<ReviewPost, Long> {
    Page<ReviewPost> findByPost_UserId(Long userId, Pageable pageable);

    @Query("""
        SELECT
            rp
        FROM
            ReviewPost rp
        JOIN FETCH
            rp.post p
        WHERE
            p.title LIKE %:keyword%
            OR p.content LIKE %:keyword%
        ORDER BY
            p.createdDate DESC
    """)
    List<ReviewPost> searchByKeyword(
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
