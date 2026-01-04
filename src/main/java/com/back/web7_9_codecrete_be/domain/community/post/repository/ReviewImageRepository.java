package com.back.web7_9_codecrete_be.domain.community.post.repository;

import com.back.web7_9_codecrete_be.domain.community.post.entity.ReviewImage;
import com.back.web7_9_codecrete_be.domain.community.post.entity.ReviewPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {
    List<ReviewImage> findByReviewPost(ReviewPost reviewPost);

    void deleteByReviewPost(ReviewPost reviewPost);
}
