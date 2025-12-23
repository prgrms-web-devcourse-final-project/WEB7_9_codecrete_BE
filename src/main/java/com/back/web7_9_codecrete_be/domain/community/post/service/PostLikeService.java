package com.back.web7_9_codecrete_be.domain.community.post.service;

import com.back.web7_9_codecrete_be.domain.community.post.entity.PostLike;
import com.back.web7_9_codecrete_be.domain.community.post.repository.PostLikeRepository;
import com.back.web7_9_codecrete_be.domain.community.post.repository.PostRepository;
import com.back.web7_9_codecrete_be.global.error.code.PostErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;

    // 좋아요 토글
    @Transactional
    public boolean toggle(Long postId, Long userId) {

        postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));

        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            postLikeRepository.deleteByPostIdAndUserId(postId, userId);
            return false; // 좋아요 취소
        }

        PostLike postLike = PostLike.create(postId, userId);
        postLikeRepository.save(postLike);

        return true; // 좋아요 등록
    }

    // 특정 게시글의 좋아요 개수 조회
    public long count(Long postId) {
        return postLikeRepository.countByPostId(postId);
    }
}
