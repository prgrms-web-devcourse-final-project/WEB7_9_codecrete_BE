package com.back.web7_9_codecrete_be.domain.community.post.service;

import com.back.web7_9_codecrete_be.domain.community.comment.dto.response.MyCommentResponse;
import com.back.web7_9_codecrete_be.domain.community.comment.entity.Comment;
import com.back.web7_9_codecrete_be.domain.community.comment.repository.CommentRepository;
import com.back.web7_9_codecrete_be.domain.community.post.dto.response.MyLikedPostResponse;
import com.back.web7_9_codecrete_be.domain.community.post.dto.response.MyPagePostResponse;
import com.back.web7_9_codecrete_be.domain.community.post.entity.Post;
import com.back.web7_9_codecrete_be.domain.community.post.entity.PostCategory;
import com.back.web7_9_codecrete_be.domain.community.post.repository.PostLikeRepository;
import com.back.web7_9_codecrete_be.domain.community.post.repository.PostRepository;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyCommunityService {

    private static final int PAGE_SIZE = 10;

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;

    // 내가 작성한 게시글 조회
    public Page<MyPagePostResponse> getMyPosts(User user, int page) {
        Page<Post> posts =
                postRepository.findByUserId(
                        user.getId(),
                        PageRequest.of(page - 1, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdDate"))
                );

        return posts.map(post ->
                MyPagePostResponse.builder()
                        .postId(post.getPostId())
                        .userId(post.getUserId())
                        .category(post.getCategory())
                        .title(post.getTitle())
                        .content(post.getContent())
                        .concertId(post.getConcertId())
                        .createdAt(post.getCreatedDate())
                        .rating(
                                post.getCategory() == PostCategory.REVIEW
                                        && post.getReviewPost() != null
                                        ? Double.valueOf(post.getReviewPost().getRating())
                                        : null
                        )
                        .build()
        );
    }

    // 내가 작성한 댓글 조회
    public Page<MyCommentResponse> getMyComments(User user, int page) {
        Page<Comment> comments =
                commentRepository.findByUserId(
                        user.getId(),
                        PageRequest.of(page - 1, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdDate"))
                );

        return comments.map(comment ->
                MyCommentResponse.builder()
                        .commentId(comment.getCommentId())
                        .postId(comment.getPost().getPostId())
                        .postTitle(comment.getPost().getTitle())
                        .content(comment.getContent())
                        .createdAt(comment.getCreatedDate())
                        .build()
        );
    }

    // 좋아요한 게시글 조회
    public Page<MyLikedPostResponse> getMyLikedPosts(User user, int page) {
        return postLikeRepository
                .findByUserId(
                        user.getId(),
                        PageRequest.of(page - 1, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdDate"))
                )
                .map(postLike -> {
                    Post post = postRepository.findById(postLike.getPostId())
                            .orElseThrow(); // 정상 데이터라면 거의 안 터짐

                    return MyLikedPostResponse.builder()
                            .postId(post.getPostId())
                            .userId(post.getUserId())
                            .category(post.getCategory())
                            .title(post.getTitle())
                            .content(post.getContent())
                            .concertId(post.getConcertId())
                            .likedAt(postLike.getCreatedDate())
                            .build();
                });
    }
}
