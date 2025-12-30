package com.back.web7_9_codecrete_be.domain.community.post.service;

import com.back.web7_9_codecrete_be.domain.community.post.dto.request.ReviewPostMultipartRequest;
import com.back.web7_9_codecrete_be.domain.community.post.dto.request.ReviewPostUpdateMultipartRequest;
import com.back.web7_9_codecrete_be.domain.community.post.dto.response.ReviewPostResponse;
import com.back.web7_9_codecrete_be.domain.community.post.entity.Post;
import com.back.web7_9_codecrete_be.domain.community.post.entity.PostCategory;
import com.back.web7_9_codecrete_be.domain.community.post.entity.ReviewImage;
import com.back.web7_9_codecrete_be.domain.community.post.entity.ReviewPost;
import com.back.web7_9_codecrete_be.domain.community.post.repository.PostRepository;
import com.back.web7_9_codecrete_be.domain.community.post.repository.ReviewPostRepository;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.global.error.code.PostErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import com.back.web7_9_codecrete_be.global.storage.FileStorageService;
import com.back.web7_9_codecrete_be.global.storage.ImageFileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewPostService {

    private final PostRepository postRepository;
    private final ReviewPostRepository reviewPostRepository;
    private final FileStorageService fileStorageService;
    private final ImageFileValidator imageFileValidator;

    // 후기 작성
    @Transactional
    public Long create(ReviewPostMultipartRequest req, User user) {

        Post post = Post.create(
                user.getId(),
                user.getNickname(),
                req.getTitle(),
                req.getContent(),
                PostCategory.REVIEW
        );

        ReviewPost reviewPost = ReviewPost.create(
                post,
                req.getConcertId(),
                req.getRating()
        );

        post.addReviewPost(reviewPost);
        postRepository.save(post);

        // 이미지 업로드
        if (req.getImages() != null) {
            for (MultipartFile file : req.getImages()) {
                imageFileValidator.validateImageFile(file);

                String url =
                        fileStorageService.upload(file, "reviews/images");

                reviewPost.addImage(
                        ReviewImage.create(reviewPost, url)
                );
            }
        }

        return post.getPostId();
    }

    // 후기 단건 조회
    @Transactional(readOnly = true)
    public ReviewPostResponse getReview(Long postId) {

        ReviewPost reviewPost = reviewPostRepository.findById(postId)
                .orElseThrow(() ->
                        new BusinessException(PostErrorCode.POST_NOT_FOUND)
                );

        List<String> imageUrls = reviewPost.getImages()
                .stream()
                .map(ReviewImage::getImageUrl)
                .toList();

        return ReviewPostResponse.from(reviewPost, imageUrls);
    }

    // 후기 수정
    @Transactional
    public void update(
            Long postId,
            ReviewPostUpdateMultipartRequest req,
            Long userId
    ) {
        ReviewPost reviewPost = reviewPostRepository.findById(postId)
                .orElseThrow(() ->
                        new BusinessException(PostErrorCode.POST_NOT_FOUND));

        Post post = validateOwner(reviewPost, userId);

        post.update(req.getTitle(), req.getContent(), PostCategory.REVIEW);
        reviewPost.updateRating(req.getRating());

        updateImages(
                reviewPost,
                req.getRemainImageUrls(),
                req.getImages()
        );
    }

    // 후기 삭제
    @Transactional
    public void delete(Long postId, Long userId) {

        ReviewPost reviewPost = reviewPostRepository.findById(postId)
                .orElseThrow(() ->
                        new BusinessException(PostErrorCode.POST_NOT_FOUND));

        Post post = validateOwner(reviewPost, userId);

        List<String> urls = reviewPost.getImages()
                .stream()
                .map(ReviewImage::getImageUrl)
                .toList();

        postRepository.delete(post);

        urls.forEach(this::safeDeleteImage);
    }

    // 이미지 업데이트
    private void updateImages(
            ReviewPost reviewPost,
            List<String> remainImageUrls,
            List<MultipartFile> newImages
    ) {
        List<String> remainUrls =
                remainImageUrls == null ? List.of() : remainImageUrls;

        // 기존 이미지 중 삭제 대상 제거
        List<ReviewImage> oldImages =
                List.copyOf(reviewPost.getImages());

        for (ReviewImage image : oldImages) {
            if (!remainUrls.contains(image.getImageUrl())) {
                reviewPost.removeImage(image);
                safeDeleteImage(image.getImageUrl());
            }
        }

        // 새 이미지 업로드
        if (newImages != null) {
            for (MultipartFile file : newImages) {
                imageFileValidator.validateImageFile(file);

                String url =
                        fileStorageService.upload(file, "reviews/images");

                reviewPost.addImage(
                        ReviewImage.create(reviewPost, url)
                );
            }
        }
    }

    // 작성자 검증
    private Post validateOwner(ReviewPost reviewPost, Long userId) {
        Post post = reviewPost.getPost();

        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(PostErrorCode.NO_POST_PERMISSION);
        }

        return post;
    }

    // 이미지 안전 삭제
    private void safeDeleteImage(String url) {
        try {
            fileStorageService.delete(url);
        } catch (Exception e) {
            log.warn("후기 이미지 삭제 실패 url={}", url, e);
        }
    }
}

