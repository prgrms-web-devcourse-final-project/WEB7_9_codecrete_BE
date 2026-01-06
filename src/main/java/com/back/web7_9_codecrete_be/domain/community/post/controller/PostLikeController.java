package com.back.web7_9_codecrete_be.domain.community.post.controller;

import com.back.web7_9_codecrete_be.domain.community.post.service.PostLikeService;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.global.rq.Rq;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts/{postId}/likes")
@RequiredArgsConstructor
@Tag(name = "Community - PostLike", description = "게시글 좋아요 API")
public class PostLikeController {

    private final PostLikeService postLikeService;
    private final Rq rq;

    @Operation(summary = "게시글 좋아요 토글", description = "좋아요를 누르거나 취소합니다.")
    @PostMapping
    public RsData<?> toggleLike(@PathVariable Long postId) {
        User user = rq.getUser();
        boolean liked = postLikeService.toggle(postId, user.getId());
        return RsData.success(
                liked ? "게시글을 좋아요 했습니다." : "게시글 좋아요를 취소했습니다.",
                liked
        );
    }

    @Operation(summary = "게시글 좋아요 수 조회", description = "게시글의 좋아요 개수를 조회합니다.")
    @GetMapping("/count")
    public RsData<?> getLikeCount(@PathVariable Long postId) {
        long count = postLikeService.count(postId);
        return RsData.success("게시글 좋아요 수 조회 성공", count);
    }

    @Operation(summary = "게시글 좋아요 여부 조회", description = "현재 로그인한 사용자가 해당 게시글을 좋아요 했는지 여부를 반환합니다.")
    @GetMapping("/me")
    public RsData<?> isLikedByMe(@PathVariable Long postId) {
        User user = rq.getUser();
        boolean liked = postLikeService.isLiked(postId, user.getId());
        return RsData.success("게시글 좋아요 여부 조회 성공", liked);
    }


}
