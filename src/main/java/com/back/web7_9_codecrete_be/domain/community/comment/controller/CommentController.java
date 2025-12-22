package com.back.web7_9_codecrete_be.domain.community.comment.controller;

import com.back.web7_9_codecrete_be.domain.community.comment.dto.request.CommentCreateRequest;
import com.back.web7_9_codecrete_be.domain.community.comment.service.CommentService;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.global.rq.Rq;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
@RequiredArgsConstructor
@Tag(name = "Community - Comment", description = "커뮤니티 댓글 API")
public class CommentController {

    private final CommentService commentService;
    private final Rq rq;

    @Operation(summary = "댓글 작성", description = "특정 게시글에 댓글을 작성합니다.")
    @PostMapping
    public RsData<?> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest req
    ) {
        User user = rq.getUser();
        Long commentId = commentService.create(postId, req, user.getId());
        return RsData.success("댓글이 작성되었습니다.", commentId);
    }

    @GetMapping
    @Operation(summary = "댓글 목록 조회", description = "게시글 댓글을 페이지 단위로 조회합니다.")
    public RsData<?> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") int page
    ) {
        return RsData.success("댓글 목록 조회 성공", commentService.getComments(postId, page));
    }

    @Operation(summary = "댓글 삭제", description = "작성자 본인만 댓글을 삭제할 수 있습니다.")
    @DeleteMapping("/{commentId}")
    public RsData<?> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        User user = rq.getUser();
        commentService.delete(commentId, user.getId());
        return RsData.success("댓글이 삭제되었습니다.");
    }
}
