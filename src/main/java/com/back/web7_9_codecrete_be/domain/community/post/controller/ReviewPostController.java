package com.back.web7_9_codecrete_be.domain.community.post.controller;


import com.back.web7_9_codecrete_be.domain.community.post.dto.request.ReviewPostMultipartRequest;
import com.back.web7_9_codecrete_be.domain.community.post.dto.request.ReviewPostUpdateMultipartRequest;
import com.back.web7_9_codecrete_be.domain.community.post.dto.response.ReviewPostResponse;
import com.back.web7_9_codecrete_be.domain.community.post.service.ReviewPostService;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.global.rq.Rq;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Community - Review", description = "후기 게시글 API")
public class ReviewPostController {

    private final ReviewPostService reviewPostService;
    private final Rq rq;

    @Operation(summary = "후기 게시글 작성")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RsData<?> createReview(
            @ModelAttribute @Valid ReviewPostMultipartRequest req
    ) {
        User user = rq.getUser();
        Long postId = reviewPostService.create(req, user);
        return RsData.success("후기 게시글이 작성되었습니다.", postId);
    }

    @Operation(summary = "후기 게시글 상세 조회")
    @GetMapping("/{postId}")
    public RsData<ReviewPostResponse> getReview(
            @PathVariable Long postId
    ) {
        return RsData.success(
                "후기 게시글 조회 성공",
                reviewPostService.getReview(postId)
        );
    }

    @Operation(summary = "후기 게시글 수정")
    @PutMapping(
            value = "/{postId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public RsData<?> updateReview(
            @PathVariable Long postId,
            @ModelAttribute @Valid ReviewPostUpdateMultipartRequest req
    ) {
        User user = rq.getUser();
        reviewPostService.update(postId, req, user.getId());
        return RsData.success("후기 게시글이 수정되었습니다.");
    }

    @Operation(summary = "후기 게시글 삭제")
    @DeleteMapping("/{postId}")
    public RsData<?> deleteReview(
            @PathVariable Long postId
    ) {
        User user = rq.getUser();
        reviewPostService.delete(postId, user.getId());
        return RsData.success("후기 게시글이 삭제되었습니다.");
    }
}
