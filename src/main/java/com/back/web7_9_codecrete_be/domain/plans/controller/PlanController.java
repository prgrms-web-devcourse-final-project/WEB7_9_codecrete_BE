package com.back.web7_9_codecrete_be.domain.plans.controller;

import com.back.web7_9_codecrete_be.domain.plans.dto.request.PlanAddRequest;
import com.back.web7_9_codecrete_be.domain.plans.dto.request.PlanUpdateRequest;
import com.back.web7_9_codecrete_be.domain.plans.dto.response.PlanDeleteResponse;
import com.back.web7_9_codecrete_be.domain.plans.dto.response.PlanDetailResponse;
import com.back.web7_9_codecrete_be.domain.plans.dto.response.PlanListResponse;
import com.back.web7_9_codecrete_be.domain.plans.dto.response.PlanResponse;
import com.back.web7_9_codecrete_be.domain.plans.service.PlanService;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
@Tag(name = "Plans", description = "계획(Plans) 관련 API")
public class PlanController {
    
    private final PlanService planService;

    /**
     * 계획 목록 조회
     * 
     * @return 계획 목록 (200 OK)
     */
    @GetMapping("/list")
    @Operation(summary = "계획 목록 조회", description = "사용자의 계획 목록을 조회합니다.")
    public RsData<List<PlanListResponse>> getPlanList(@RequestParam Long userId) {
        List<PlanListResponse> planList = planService.getPlanList(userId);
        return RsData.success("계획 목록 조회 성공", planList);
    }

    /**
     * 계획 상세 조회
     * 
     * @param planId 계획 ID
     * @return 계획 상세 정보 (200 OK)
     */
    @GetMapping("/{planId}")
    @Operation(summary = "계획 상세 조회", description = "특정 계획의 상세 정보를 조회합니다.")
    public RsData<PlanDetailResponse> getPlanDetail(@PathVariable Long planId,
                                                    @RequestParam Long userId) {
        PlanDetailResponse planDetail = planService.getPlanDetail(planId, userId);
        return RsData.success("계획 상세 조회 성공", planDetail);
    }

    /**
     * 계획 생성
     * 
     * @param concertId 콘서트 ID
     * @param request 계획 생성 요청 DTO
     * @return 생성된 계획 정보 (201 Created)
     */
    @PostMapping("/{concertId}")
    @Operation(summary = "계획 생성", description = "새로운 계획을 생성합니다.")
    public RsData<PlanResponse> createPlan(
            @PathVariable Long concertId,
            @Valid @RequestBody PlanAddRequest request) {
        PlanResponse planResponse = planService.createPlan(concertId, request);
        return RsData.success(HttpStatus.CREATED, "계획 생성 성공", planResponse);
    }

    /**
     * 계획 수정
     *
     * @param planId 계획 ID
     * @param request 계획 수정 요청 DTO
     * @return 수정된 계획 정보 (200 OK)
     */
    @PatchMapping("/update/{planId}")
    @Operation(summary = "계획 수정", description = "기존 계획의 정보를 수정합니다.")
    public RsData<PlanResponse> updatePlan(
            @PathVariable Long planId,
            @Valid @RequestBody PlanUpdateRequest request) {
        PlanResponse planResponse = planService.updatePlan(planId, request);
        return RsData.success("계획 수정 성공", planResponse);
    }

    /**
     * 계획 삭제
     *
     * @param planId 계획 ID
     * @return 삭제된 계획 ID (200 OK)
     */
    @DeleteMapping("/delete/{planId}")
    @Operation(summary = "계획 삭제", description = "기존 계획을 삭제합니다.")
    public RsData<PlanDeleteResponse> deletePlan(@PathVariable Long planId) {
        PlanDeleteResponse deleteResponse = planService.deletePlan(planId);
        return RsData.success("계획 삭제 성공", deleteResponse);
    }


    // POST /api/v1/plans/invite/{planID} - 계획 공유 초대
    // POST /api/v1/plans/accept/{planID} - 계획 공유 수락
    // POST /api/v1/plans/deny/{planID} - 계획 공유 거절
    // DELETE /api/v1/plans/kick/{planID} - 계획 공유 인원 추방
    // DELETE /api/v1/plans/quit/{planID} - 계획 공유 나가기
}