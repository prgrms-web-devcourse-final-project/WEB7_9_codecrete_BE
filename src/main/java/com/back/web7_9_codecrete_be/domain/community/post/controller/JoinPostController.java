package com.back.web7_9_codecrete_be.domain.community.post.controller;

import com.back.web7_9_codecrete_be.domain.community.post.dto.request.JoinPostCreateRequest;
import com.back.web7_9_codecrete_be.domain.community.post.dto.request.JoinPostUpdateRequest;
import com.back.web7_9_codecrete_be.domain.community.post.dto.response.JoinPostResponse;
import com.back.web7_9_codecrete_be.domain.community.post.service.JoinPostService;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.global.rq.Rq;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/join")
@RequiredArgsConstructor
@Tag(name = "Community - Join", description = "구인글 API")
public class JoinPostController {

    private final JoinPostService joinPostService;
    private final Rq rq;

    @Operation(summary = "구인글 작성")
    @PostMapping
    public RsData<?> create(
            @Valid @RequestBody JoinPostCreateRequest req
    ) {
        User user = rq.getUser();
        Long postId = joinPostService.create(req, user);
        return RsData.success("구인글이 작성되었습니다.", postId);
    }

    @Operation(summary = "구인글 상세 조회")
    @GetMapping("/{postId}")
    public RsData<JoinPostResponse> get(
            @PathVariable Long postId
    ) {
        return RsData.success(
                "구인글 조회 성공",
                joinPostService.get(postId)
        );
    }

    @Operation(summary = "구인글 수정")
    @PutMapping("/{postId}")
    public RsData<?> update(
            @PathVariable Long postId,
            @Valid @RequestBody JoinPostUpdateRequest req
    ) {
        User user = rq.getUser();
        joinPostService.update(postId, req, user.getId());
        return RsData.success("구인글이 수정되었습니다.");
    }

    @Operation(summary = "구인글 삭제")
    @DeleteMapping("/{postId}")
    public RsData<?> delete(
            @PathVariable Long postId
    ) {
        User user = rq.getUser();
        joinPostService.delete(postId, user.getId());
        return RsData.success("구인글이 삭제되었습니다.");
    }

    @Operation(summary = "구인글 마감")
    @PatchMapping("/{postId}/close")
    public RsData<?> close(
            @PathVariable Long postId
    ) {
        User user = rq.getUser();
        joinPostService.close(postId, user.getId());
        return RsData.success("구인글이 마감되었습니다.");
    }
}
