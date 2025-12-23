package com.back.web7_9_codecrete_be.domain.community.post.controller;

import com.back.web7_9_codecrete_be.domain.community.post.dto.request.PostCreateRequest;
import com.back.web7_9_codecrete_be.domain.community.post.dto.request.PostUpdateRequest;
import com.back.web7_9_codecrete_be.domain.community.post.dto.response.PostPageResponse;
import com.back.web7_9_codecrete_be.domain.community.post.dto.response.PostResponse;
import com.back.web7_9_codecrete_be.domain.community.post.entity.PostCategory;
import com.back.web7_9_codecrete_be.domain.community.post.service.PostService;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.global.rq.Rq;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Tag(name = "Community - Post", description = "커뮤니티 게시글 API")
public class PostController {

    private final PostService postService;
    private final Rq rq;

    @Operation(summary = "게시글 작성", description = "카테고리, 제목, 내용을 입력하여 게시글을 작성합니다.")
    @PostMapping
    public RsData<?> createPost(@Valid @RequestBody PostCreateRequest req) {
        User user = rq.getUser();
        Long postId = postService.create(req, user);
        return RsData.success("게시글이 작성되었습니다.", postId);
    }

    @Operation(summary = "게시글 단건 조회", description = "게시글 ID로 게시글 상세 정보를 조회합니다.")
    @GetMapping("/{postId}")
    public RsData<?> getPost(@PathVariable Long postId) {
        PostResponse response = postService.getPost(postId);
        return RsData.success("게시글 조회 성공", response);
    }

    @GetMapping
    @Operation(summary = "게시글 목록 조회", description = "커뮤니티 게시글을 페이지 단위로 조회합니다.")
    public RsData<PostPageResponse<PostResponse>> getPosts(
            @RequestParam(defaultValue = "1") int page
    ) {
        return RsData.success("게시글 목록 조회 성공", postService.getPosts(page));
    }

    @GetMapping("/category/{category}")
    @Operation(
            summary = "카테고리별 게시글 조회",
            description = "특정 카테고리에 속한 게시글을 페이지 단위로 조회합니다."
    )
    public RsData<PostPageResponse<PostResponse>> getPostsByCategory(
            @PathVariable PostCategory category,
            @RequestParam(defaultValue = "1") int page
    ) {
        return RsData.success("카테고리별 게시글 조회 성공", postService.getPostsByCategory(page, category));
    }

    @Operation(summary = "게시글 수정", description = "작성자 본인만 게시글을 수정할 수 있습니다.")
    @PutMapping("/{postId}")
    public RsData<?> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest req
    ) {
        User user = rq.getUser();
        postService.update(postId, req, user.getId());
        return RsData.success("게시글이 수정되었습니다.");
    }

    @Operation(summary = "게시글 삭제", description = "작성자 본인만 게시글을 삭제할 수 있습니다.")
    @DeleteMapping("/{postId}")
    public RsData<?> deletePost(@PathVariable Long postId) {
        User user = rq.getUser();
        postService.delete(postId, user.getId());
        return RsData.success("게시글이 삭제되었습니다.");
    }
}
