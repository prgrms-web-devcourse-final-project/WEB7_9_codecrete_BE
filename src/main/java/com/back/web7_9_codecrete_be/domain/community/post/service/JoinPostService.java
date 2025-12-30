package com.back.web7_9_codecrete_be.domain.community.post.service;

import com.back.web7_9_codecrete_be.domain.community.post.dto.request.JoinPostCreateRequest;
import com.back.web7_9_codecrete_be.domain.community.post.dto.request.JoinPostUpdateRequest;
import com.back.web7_9_codecrete_be.domain.community.post.dto.response.JoinPostResponse;
import com.back.web7_9_codecrete_be.domain.community.post.entity.JoinPost;
import com.back.web7_9_codecrete_be.domain.community.post.entity.JoinStatus;
import com.back.web7_9_codecrete_be.domain.community.post.entity.Post;
import com.back.web7_9_codecrete_be.domain.community.post.entity.PostCategory;
import com.back.web7_9_codecrete_be.domain.community.post.repository.JoinPostRepository;
import com.back.web7_9_codecrete_be.domain.community.post.repository.PostRepository;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.global.error.code.PostErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JoinPostService {

    private final PostRepository postRepository;
    private final JoinPostRepository joinPostRepository;

    // 구인글 작성
    @Transactional
    public Long create(JoinPostCreateRequest req, User user) {

        Post post = Post.create(
                user.getId(),
                user.getNickname(),
                req.getTitle(),
                req.getContent(),
                PostCategory.JOIN
        );

        JoinPost joinPost = JoinPost.create(post, req);
        post.addJoinPost(joinPost);

        postRepository.save(post);
        return post.getPostId();
    }

    // 구인글 단건 조회
    @Transactional(readOnly = true)
    public JoinPostResponse get(Long postId) {
        JoinPost joinPost = joinPostRepository.findById(postId)
                .orElseThrow(() ->
                        new BusinessException(PostErrorCode.POST_NOT_FOUND)
                );

        return JoinPostResponse.from(joinPost);
    }

    // 구인글 수정
    @Transactional
    public void update(
            Long postId,
            JoinPostUpdateRequest req,
            Long userId
    ) {
        JoinPost joinPost = joinPostRepository.findById(postId)
                .orElseThrow(() ->
                        new BusinessException(PostErrorCode.POST_NOT_FOUND)
                );

        Post post = validateOwner(joinPost, userId);

        post.update(req.getTitle(), req.getContent(), PostCategory.JOIN);

        joinPost.update(req);
    }

    // 구인글 삭제
    @Transactional
    public void delete(Long postId, Long userId) {
        JoinPost joinPost = joinPostRepository.findById(postId)
                .orElseThrow(() ->
                        new BusinessException(PostErrorCode.POST_NOT_FOUND)
                );

        Post post = validateOwner(joinPost, userId);
        postRepository.delete(post);
    }

    // 구인글 작성자 검증
    private Post validateOwner(JoinPost joinPost, Long userId) {
        Post post = joinPost.getPost();

        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(PostErrorCode.NO_POST_PERMISSION);
        }
        return post;
    }

    @Transactional
    public void close(Long postId, Long userId) {
        JoinPost joinPost = joinPostRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));

        Post post = joinPost.getPost();

        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(PostErrorCode.NO_POST_PERMISSION);
        }

        if (joinPost.getStatus() == JoinStatus.CLOSED) {
            throw new BusinessException(PostErrorCode.JOIN_ALREADY_CLOSED);
        }

        joinPost.close(); // status = CLOSED
    }
}
