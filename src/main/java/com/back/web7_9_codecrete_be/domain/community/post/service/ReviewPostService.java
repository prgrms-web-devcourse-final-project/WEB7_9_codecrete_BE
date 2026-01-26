package com.back.web7_9_codecrete_be.domain.community.post.service;

import com.back.web7_9_codecrete_be.domain.community.post.dto.request.ReviewPostMultipartRequest;
import com.back.web7_9_codecrete_be.domain.community.post.dto.request.ReviewPostUpdateMultipartRequest;
import com.back.web7_9_codecrete_be.domain.community.post.dto.response.ConcertReviewListResponse;
import com.back.web7_9_codecrete_be.domain.community.post.dto.response.ReviewItemResponse;
import com.back.web7_9_codecrete_be.domain.community.post.dto.response.ReviewPostResponse;
import com.back.web7_9_codecrete_be.domain.community.post.dto.response.ReviewSummary;
import com.back.web7_9_codecrete_be.domain.community.post.entity.Post;
import com.back.web7_9_codecrete_be.domain.community.post.entity.PostCategory;
import com.back.web7_9_codecrete_be.domain.community.post.entity.ReviewImage;
import com.back.web7_9_codecrete_be.domain.community.post.entity.ReviewPost;
import com.back.web7_9_codecrete_be.domain.community.post.repository.PostRepository;
import com.back.web7_9_codecrete_be.domain.community.post.repository.ReviewPostRepository;
import com.back.web7_9_codecrete_be.domain.concerts.service.ConcertService;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.global.error.code.PostErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import com.back.web7_9_codecrete_be.global.storage.FileStorageService;
import com.back.web7_9_codecrete_be.global.storage.ImageFileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewPostService {

    private final PostRepository postRepository;
    private final ReviewPostRepository reviewPostRepository;
    private final FileStorageService fileStorageService;
    private final ImageFileValidator imageFileValidator;
    private final PostLikeService postLikeService;
    private final ConcertService concertService;

    // 후기 작성
    @Transactional
    public Long create(ReviewPostMultipartRequest req, User user) {

        concertService.validateConcertExists(req.getConcertId());

        Post post = Post.create(
                user.getId(),
                req.getConcertId(),
                req.getTitle(),
                req.getContent(),
                PostCategory.REVIEW
        );

        ReviewPost reviewPost = ReviewPost.create(
                post,
                req
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

        concertService.validateConcertExists(req.getConcertId());

        post.update(req.getConcertId(), req.getTitle(), req.getContent(), PostCategory.REVIEW);
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

    @Transactional(readOnly = true)
    public ConcertReviewListResponse getReviewsByConcert(Long concertId) {

        List<Post> posts =
                postRepository.findByConcertIdAndCategory(
                        concertId,
                        PostCategory.REVIEW
                );

        List<ReviewPost> reviews = posts.stream()
                .map(Post::getReviewPost)
                .toList();

        // 1. 요약 계산
        ReviewSummary summary = buildSummary(reviews);

        // 2. 목록 변환
        List<ReviewItemResponse> items = reviews.stream()
                .map(this::toReviewItem)
                .toList();

        return ConcertReviewListResponse.builder()
                .summary(summary)
                .reviews(items)
                .build();
    }

    private ReviewSummary buildSummary(List<ReviewPost> reviews) {

        long total = reviews.size();

        double avg = total == 0
                ? 0.0
                : reviews.stream()
                .mapToInt(ReviewPost::getRating)
                .average()
                .orElse(0.0);

        Map<Integer, Long> distribution =
                reviews.stream()
                        .collect(Collectors.groupingBy(
                                ReviewPost::getRating,
                                Collectors.counting()
                        ));

        // 1~5점 보정
        for (int i = 1; i <= 5; i++) {
            distribution.putIfAbsent(i, 0L);
        }

        return ReviewSummary.builder()
                .totalCount(total)
                .averageRating(Math.round(avg * 10) / 10.0)
                .ratingDistribution(distribution)
                .build();
    }

    private ReviewItemResponse toReviewItem(ReviewPost reviewPost) {

        Long postId = reviewPost.getPost().getPostId();

        return ReviewItemResponse.builder()
                .postId(postId)
                .userId(reviewPost.getPost().getUserId())
                .title(reviewPost.getPost().getTitle())
                .content(reviewPost.getPost().getContent())
                .rating(reviewPost.getRating())
                .likeCount(postLikeService.count(postId))
                .tags(reviewPost.getTags())
                .createdDate(reviewPost.getPost().getCreatedDate())
                .build();
    }

    public List<ReviewPostResponse> searchByKeyword(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException(PostErrorCode.KEYWORD_IS_NULL);
        }

        return reviewPostRepository.searchByKeyword(keyword, pageable)
                .stream()
                .map(reviewPost -> {
                    List<String> imageUrls = reviewPost.getImages()
                            .stream()
                            .map(ReviewImage::getImageUrl)
                            .toList();

                    return ReviewPostResponse.from(reviewPost, imageUrls);
                })
                .toList();
    }
}

