package com.back.web7_9_codecrete_be.domain.community.comment.service;

import com.back.web7_9_codecrete_be.domain.community.comment.dto.request.CommentCreateRequest;
import com.back.web7_9_codecrete_be.domain.community.comment.dto.request.CommentUpdateRequest;
import com.back.web7_9_codecrete_be.domain.community.comment.dto.response.CommentPageResponse;
import com.back.web7_9_codecrete_be.domain.community.comment.dto.response.CommentResponse;
import com.back.web7_9_codecrete_be.domain.community.comment.entity.Comment;
import com.back.web7_9_codecrete_be.domain.community.comment.repository.CommentRepository;
import com.back.web7_9_codecrete_be.domain.community.post.entity.Post;
import com.back.web7_9_codecrete_be.domain.community.post.service.PostService;
import com.back.web7_9_codecrete_be.global.error.code.CommentErrorCode;
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
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostService postService;

    // 댓글 생성
    @Transactional
    public Long create(Long postId, CommentCreateRequest req, Long userId) {
        Post post = postService.getPostOrThrow(postId);

        Comment comment = Comment.create(
                post,
                userId,
                req.getContent()
        );

        return commentRepository.save(comment).getCommentId();
    }

    // 댓글 조회
    public CommentPageResponse<CommentResponse> getComments(Long postId, int page) {

        postService.validatePostExists(postId);

        Pageable pageable = PageRequest.of(
                page - 1,
                20,
                Sort.by(Sort.Direction.ASC, "createdDate")
        );

        Page<CommentResponse> result =
                commentRepository.findByPost_PostId(postId, pageable)
                        .map(CommentResponse::from);

        return CommentPageResponse.from(result);
    }

    // 댓글 삭제
    @Transactional
    public void delete(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND));

        validateOwner(comment, userId);

        commentRepository.delete(comment);
    }

    @Transactional
    public void update(Long commentId, CommentUpdateRequest req, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND));

        validateOwner(comment, userId);

        comment.update(req.getContent());
    }

    private void validateOwner(Comment comment, Long userId) {
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(CommentErrorCode.NO_COMMENT_PERMISSION);
        }
    }
}
