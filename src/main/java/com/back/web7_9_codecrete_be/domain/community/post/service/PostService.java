package com.back.web7_9_codecrete_be.domain.community.post.service;

import com.back.web7_9_codecrete_be.domain.community.post.dto.request.PostCreateRequest;
import com.back.web7_9_codecrete_be.domain.community.post.dto.request.PostUpdateRequest;
import com.back.web7_9_codecrete_be.domain.community.post.dto.response.PostPageResponse;
import com.back.web7_9_codecrete_be.domain.community.post.dto.response.PostResponse;
import com.back.web7_9_codecrete_be.domain.community.post.entity.Post;
import com.back.web7_9_codecrete_be.domain.community.post.entity.PostCategory;
import com.back.web7_9_codecrete_be.domain.community.post.repository.PostRepository;
import com.back.web7_9_codecrete_be.domain.concerts.service.ConcertService;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.global.error.code.PostErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final ConcertService concertService;

    // 게시글 작성
    @Transactional
    public Long create(PostCreateRequest req, User user) {

        concertService.validateConcertExists(req.getConcertId());

        Post post = Post.create(
                user.getId(),
                req.getConcertId(),
                req.getTitle(),
                req.getContent(),
                req.getCategory()
        );

        return postRepository.save(post).getPostId();
    }

    @Transactional(readOnly = true)
    // 게시글 단건 조회
    public PostResponse getPost(Long postId) {
        Post post = findPost(postId);
        return PostResponse.from(post);
    }

    @Transactional(readOnly = true)
    // 게시글 전체 조회
    public PostPageResponse<PostResponse> getPosts(int page) {

        Pageable pageable = PageRequest.of(
                page - 1,
                10,
                Sort.by(Sort.Direction.DESC, "createdDate")
        );

        Page<PostResponse> result = postRepository.findAll(pageable)
                .map(PostResponse::from);

        return PostPageResponse.from(result);
    }

    // 게시글 수정
    @Transactional
    public void update(Long postId, PostUpdateRequest req, Long userId) {
        Post post = findPost(postId);
        validateOwner(post, userId);

        concertService.validateConcertExists(req.getConcertId());

        post.update(
                req.getConcertId(),
                req.getTitle(),
                req.getContent(),
                req.getCategory()
        );
    }

    // 게시글 삭제
    @Transactional
    public void delete(Long postId, Long userId) {
        Post post = findPost(postId);
        validateOwner(post, userId);

        postRepository.delete(post);
    }

    // 게시글 단건 조회 (예외처리 포함)
    private Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));
    }

    // 게시글 작성자 검증
    private void validateOwner(Post post, Long userId) {
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(PostErrorCode.NO_POST_PERMISSION);
        }
    }

    @Transactional(readOnly = true)
    public PostPageResponse<PostResponse> getPostsByCategory(
            int page,
            PostCategory category
    ) {
        Pageable pageable = PageRequest.of(
                page - 1,
                10,
                Sort.by(Sort.Direction.DESC, "createdDate")
        );

        Page<PostResponse> result =
                postRepository.findByCategory(category, pageable)
                        .map(PostResponse::from);

        return PostPageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public void validatePostExists(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new BusinessException(PostErrorCode.POST_NOT_FOUND);
        }
    }

    @Transactional(readOnly = true)
    public Post getPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));
    }
}
