package com.back.web7_9_codecrete_be.domain.plans.controller;

import com.back.web7_9_codecrete_be.domain.plans.dto.request.PlanAddRequest;
import com.back.web7_9_codecrete_be.domain.plans.dto.request.PlanParticipantRoleUpdateRequest;
import com.back.web7_9_codecrete_be.domain.plans.dto.request.PlanUpdateRequest;
import com.back.web7_9_codecrete_be.domain.plans.dto.request.ScheduleAddRequest;
import com.back.web7_9_codecrete_be.domain.plans.dto.request.ScheduleUpdateRequest;
import com.back.web7_9_codecrete_be.domain.plans.dto.response.PlanDeleteResponse;
import com.back.web7_9_codecrete_be.domain.plans.dto.response.PlanDetailResponse;
import com.back.web7_9_codecrete_be.domain.plans.dto.response.PlanListResponse;
import com.back.web7_9_codecrete_be.domain.plans.dto.response.PlanResponse;
import com.back.web7_9_codecrete_be.domain.plans.dto.response.ScheduleResponse;
import com.back.web7_9_codecrete_be.domain.plans.dto.response.ScheduleListResponse;
import com.back.web7_9_codecrete_be.domain.plans.dto.response.ScheduleDeleteResponse;
import com.back.web7_9_codecrete_be.domain.plans.service.PlanService;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.global.rq.Rq;
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
    private final Rq rq;

    /**
     * 계획 목록 조회
     * 
     * @return 계획 목록 (200 OK)
     */
    @GetMapping("/list")
    @Operation(summary = "계획 목록 조회", description = "사용자의 계획 목록을 조회합니다.")
    public RsData<List<PlanListResponse>> getPlanList() {
        User user = rq.getUser();
        List<PlanListResponse> planList = planService.getPlanList(user);
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
    public RsData<PlanDetailResponse> getPlanDetail(@PathVariable Long planId) {
        User user = rq.getUser();
        PlanDetailResponse planDetail = planService.getPlanDetail(planId, user);
        return RsData.success("계획 상세 조회 성공", planDetail);
    }

    /**
     * 계획 생성
     * 
     * @param request 계획 생성 요청 DTO
     * @return 생성된 계획 정보 (201 Created)
     */
    @PostMapping
    @Operation(summary = "계획 생성", description = "새로운 계획을 생성합니다.")
    public RsData<PlanResponse> createPlan(
            @Valid @RequestBody PlanAddRequest request) {
        User user = rq.getUser();
        PlanResponse planResponse = planService.createPlan(user, request);
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
        User user = rq.getUser();
        PlanResponse planResponse = planService.updatePlan(planId, user, request);
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
    public RsData<PlanDeleteResponse> deletePlan(
            @PathVariable Long planId) {
        User user = rq.getUser();
        PlanDeleteResponse deleteResponse = planService.deletePlan(planId, user);
        return RsData.success("계획 삭제 성공", deleteResponse);
    }


    /**
     * 일정 추가
     *
     * @param planId 계획 ID
     * @param request 일정 추가 요청 DTO
     * @return 생성된 일정 정보 (201 Created)
     */
    @PostMapping("/{planId}/schedules")
    @Operation(summary = "일정 추가", description = "계획에 새로운 일정을 추가합니다.")
    public RsData<ScheduleResponse> addSchedule(
            @PathVariable Long planId,
            @Valid @RequestBody ScheduleAddRequest request) {
        User user = rq.getUser();
        ScheduleResponse response = planService.addSchedule(planId, user, request);
        return RsData.success(HttpStatus.CREATED, "일정 추가 성공", response);
    }

    /**
     * 일정 목록 조회 (타임라인)
     *
     * @param planId 계획 ID
     * @return 일정 목록 (200 OK)
     */
    @GetMapping("/{planId}/schedules")
    @Operation(summary = "일정 목록 조회", description = "계획의 일정 목록을 타임라인 형태로 조회합니다.")
    public RsData<ScheduleListResponse> getSchedules(
            @PathVariable Long planId) {
        User user = rq.getUser();
        ScheduleListResponse response = planService.getSchedules(planId, user);
        return RsData.success("일정 목록 조회 성공", response);
    }

    /**
     * 일정 상세 조회
     *
     * @param planId 계획 ID
     * @param scheduleId 일정 ID
     * @return 일정 상세 정보 (200 OK)
     */
    @GetMapping("/{planId}/schedules/{scheduleId}")
    @Operation(summary = "일정 상세 조회", description = "특정 일정의 상세 정보를 조회합니다.")
    public RsData<ScheduleResponse> getSchedule(
            @PathVariable Long planId,
            @PathVariable Long scheduleId) {
        User user = rq.getUser();
        ScheduleResponse response = planService.getSchedule(planId, scheduleId, user);
        return RsData.success("일정 상세 조회 성공", response);
    }

    /**
     * 일정 수정
     *
     * @param planId 계획 ID
     * @param scheduleId 일정 ID
     * @param request 일정 수정 요청 DTO
     * @return 수정된 일정 정보 (200 OK)
     */
    @PatchMapping("/{planId}/schedules/{scheduleId}")
    @Operation(summary = "일정 수정", description = "기존 일정의 정보를 수정합니다.")
    public RsData<ScheduleResponse> updateSchedule(
            @PathVariable Long planId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleUpdateRequest request) {
        User user = rq.getUser();
        ScheduleResponse response = planService.updateSchedule(planId, scheduleId, user, request);
        return RsData.success("일정 수정 성공", response);
    }

    /**
     * 일정 삭제
     *
     * @param planId 계획 ID
     * @param scheduleId 일정 ID
     * @return 삭제된 일정 ID (200 OK)
     */
    @DeleteMapping("/{planId}/schedules/{scheduleId}")
    @Operation(summary = "일정 삭제", description = "기존 일정을 삭제합니다.")
    public RsData<ScheduleDeleteResponse> deleteSchedule(
            @PathVariable Long planId,
            @PathVariable Long scheduleId) {
        User user = rq.getUser();
        ScheduleDeleteResponse response = planService.deleteSchedule(planId, scheduleId, user);
        return RsData.success("일정 삭제 성공", response);
    }

    /**
     * 참가자 역할 수정
     *
     * @param planId 계획 ID
     * @param participantId 참가자 ID
     * @param request 역할 수정 요청 DTO
     * @return 성공 메시지 (200 OK)
     */
    @PatchMapping("/{planId}/participants/{participantId}/role")
    @Operation(summary = "참가자 역할 수정", description = "참가자의 역할을 수정합니다. (Owner, Editor, Viewer)")
    public RsData<Void> updateParticipantRole(
            @PathVariable Long planId,
            @PathVariable Long participantId,
            @Valid @RequestBody PlanParticipantRoleUpdateRequest request) {
        User user = rq.getUser();
        planService.updateParticipantRole(planId, participantId, user, request);
        return RsData.success("참가자 역할 수정 성공", null);
    }

    // POST /api/v1/plans/invite/{planID} - 계획 공유 초대
    // POST /api/v1/plans/accept/{planID} - 계획 공유 수락
    // POST /api/v1/plans/deny/{planID} - 계획 공유 거절
    // DELETE /api/v1/plans/kick/{planID} - 계획 공유 인원 추방
    // DELETE /api/v1/plans/quit/{planID} - 계획 공유 나가기
}