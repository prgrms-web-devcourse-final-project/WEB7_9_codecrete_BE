package com.back.web7_9_codecrete_be.domain.community.post.repository;

import com.back.web7_9_codecrete_be.domain.community.post.entity.Post;
import com.back.web7_9_codecrete_be.domain.community.post.entity.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {



    Page<Post> findByCategory(PostCategory category, Pageable pageable);
}
