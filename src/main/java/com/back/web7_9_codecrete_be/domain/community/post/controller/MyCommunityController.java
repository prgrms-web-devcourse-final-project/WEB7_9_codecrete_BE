package com.back.web7_9_codecrete_be.domain.community.post.controller;

import com.back.web7_9_codecrete_be.domain.community.comment.dto.response.MyCommentResponse;
import com.back.web7_9_codecrete_be.domain.community.post.dto.response.MyLikedPostResponse;
import com.back.web7_9_codecrete_be.domain.community.post.dto.response.MyPagePostResponse;
import com.back.web7_9_codecrete_be.domain.community.post.service.MyCommunityService;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.global.rq.Rq;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/community/me")
@RequiredArgsConstructor
@Tag(name = "Community - My", description = "내 커뮤니티 활동 조회 API")
public class MyCommunityController {

    private final MyCommunityService myCommunityService;
    private final Rq rq;

    @Operation(summary = "내가 작성한 게시글 조회")
    @GetMapping("/posts")
    public RsData<Page<MyPagePostResponse>> getMyPosts(
            @RequestParam(defaultValue = "1") int page
    ) {
        User user = rq.getUser();
        return RsData.success(
                "내가 작성한 게시글 조회 성공",
                myCommunityService.getMyPosts(user, page)
        );
    }

    @Operation(summary = "내가 작성한 댓글 조회")
    @GetMapping("/comments")
    public RsData<Page<MyCommentResponse>> getMyComments(
            @RequestParam(defaultValue = "1") int page
    ) {
        User user = rq.getUser();
        return RsData.success(
                "내가 작성한 댓글 조회 성공",
                myCommunityService.getMyComments(user, page)
        );
    }

    @Operation(summary = "내가 좋아요한 게시글 조회")
    @GetMapping("/liked-posts")
    public RsData<Page<MyLikedPostResponse>> getMyLikedPosts(
            @RequestParam(defaultValue = "1") int page
    ) {
        User user = rq.getUser();
        return RsData.success(
                "내가 좋아요한 게시글 조회 성공",
                myCommunityService.getMyLikedPosts(user, page)
        );
    }
}
