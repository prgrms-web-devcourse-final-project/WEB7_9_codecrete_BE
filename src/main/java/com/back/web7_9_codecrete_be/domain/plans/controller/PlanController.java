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
    @Operation(summary = "계획 상세 조회", description = "특정 계획의 상세 정보를 조회합니다. 계획에 대한 모든 세부 정보와 참가자 목록, 일정 목록을 포함합니다.")
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
        return RsData.success("계획 생성 성공", planResponse);
    }

    /**
     * 계획 수정
     *
     * @param planId 계획 ID
     * @param request 계획 수정 요청 DTO
     * @return 수정된 계획 정보 (200 OK)
     */
    @PatchMapping("/update/{planId}")
    @Operation(summary = "계획 수정", description = "기존 계획의 제목, 날짜 등의 정보를 수정합니다. 계획의 소유자 또는 편집 권한이 있는 사용자만 수정할 수 있습니다.")
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
    @Operation(summary = "계획 삭제", description = "기존 계획을 삭제합니다. 계획의 소유자만 삭제할 수 있으며, 삭제 시 관련된 모든 일정과 참가자 정보도 함께 삭제됩니다.")
    public RsData<PlanDeleteResponse> deletePlan(@PathVariable Long planId) {
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
    @Operation(summary = "일정 추가", description = "계획에 새로운 일정을 추가합니다. 일정 타입(이동수단, 식사, 대기, 활동)에 따라 필요한 정보가 다릅니다. 이동수단 타입의 경우 출발지/도착지 좌표와 이동수단 종류가 필수입니다.")
    public RsData<ScheduleResponse> addSchedule(
            @PathVariable Long planId,
            @Valid @RequestBody ScheduleAddRequest request) {
        User user = rq.getUser();
        ScheduleResponse response = planService.addSchedule(planId, user, request);
        return RsData.success("일정 추가 성공", response);
    }

    /**
     * 일정 목록 조회 (타임라인)
     *
     * @param planId 계획 ID
     * @return 일정 목록 (200 OK)
     */
    @GetMapping("/{planId}/schedules")
    @Operation(summary = "일정 목록 조회", description = "계획의 일정 목록을 타임라인 형태로 조회합니다. 시작 시간 순으로 정렬된 일정 목록을 반환합니다.")
    public RsData<ScheduleListResponse> getSchedules(@PathVariable Long planId) {
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
    @Operation(summary = "일정 상세 조회", description = "특정 일정의 상세 정보를 조회합니다. 일정의 모든 속성과 위치 정보를 포함합니다.")
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
    @Operation(summary = "일정 수정", description = "기존 일정의 정보를 수정합니다. 계획의 소유자 또는 편집 권한이 있는 사용자만 수정할 수 있습니다.")
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
    @Operation(summary = "일정 삭제", description = "기존 일정을 삭제합니다. 계획의 소유자 또는 편집 권한이 있는 사용자만 삭제할 수 있습니다.")
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
    @Operation(summary = "참가자 역할 수정", description = "참가자의 역할을 수정합니다. Owner(소유자), Editor(편집자), Viewer(조회자) 중 하나로 설정할 수 있습니다. 계획의 소유자만 다른 참가자의 역할을 수정할 수 있습니다.")
    public RsData<Void> updateParticipantRole(
            @PathVariable Long planId,
            @PathVariable Long participantId,
            @Valid @RequestBody PlanParticipantRoleUpdateRequest request) {
        User user = rq.getUser();
        planService.updateParticipantRole(planId, participantId, user, request);
        return RsData.success("참가자 역할 수정 성공", null);
    }

    /**
     * 계획 공유 초대
     *
     * @param planId 계획 ID
     * @return 성공 메시지 (200 OK)
     */
    @PostMapping("/invite/{planId}")
    @Operation(summary = "계획 공유 초대", description = "다른 사용자에게 계획 공유를 초대합니다. 계획의 소유자 또는 편집 권한이 있는 사용자만 초대할 수 있습니다. 초대된 사용자는 수락 또는 거절할 수 있습니다.")
    public RsData<Void> invitePlan(@PathVariable Long planId) {
        User user = rq.getUser();
        // TODO: 구현 필요
        return RsData.success("계획 공유 초대 성공", null);
    }

    /**
     * 계획 공유 수락
     *
     * @param planId 계획 ID
     * @return 성공 메시지 (200 OK)
     */
    @PostMapping("/accept/{planId}")
    @Operation(summary = "계획 공유 수락", description = "받은 계획 공유 초대를 수락합니다. 수락 시 해당 계획에 참가자로 추가되며, 초대 시 설정된 역할(Editor 또는 Viewer)로 참여하게 됩니다.")
    public RsData<Void> acceptPlanInvite(@PathVariable Long planId) {
        User user = rq.getUser();
        // TODO: 구현 필요
        return RsData.success("계획 공유 수락 성공", null);
    }

    /**
     * 계획 공유 거절
     *
     * @param planId 계획 ID
     * @return 성공 메시지 (200 OK)
     */
    @PostMapping("/deny/{planId}")
    @Operation(summary = "계획 공유 거절", description = "받은 계획 공유 초대를 거절합니다. 거절 시 해당 계획에 참가자로 추가되지 않으며, 초대 상태가 거절로 변경됩니다.")
    public RsData<Void> denyPlanInvite(@PathVariable Long planId) {
        User user = rq.getUser();
        // TODO: 구현 필요
        return RsData.success("계획 공유 거절 성공", null);
    }

    /**
     * 계획 공유 인원 추방
     *
     * @param planId 계획 ID
     * @param participantId 참가자 ID
     * @return 성공 메시지 (200 OK)
     */
    @DeleteMapping("/kick/{planId}/{participantId}")
    @Operation(summary = "계획 공유 인원 추방", description = "계획에 참여 중인 사용자를 추방합니다. 계획의 소유자만 다른 참가자를 추방할 수 있으며, 소유자 자신은 추방할 수 없습니다.")
    public RsData<Void> kickParticipant(
            @PathVariable Long planId,
            @PathVariable Long participantId) {
        User user = rq.getUser();
        // TODO: 구현 필요
        return RsData.success("참가자 추방 성공", null);
    }

    /**
     * 계획 공유 나가기
     *
     * @param planId 계획 ID
     * @return 성공 메시지 (200 OK)
     */
    @DeleteMapping("/quit/{planId}")
    @Operation(summary = "계획 공유 나가기", description = "공유된 계획에서 나갑니다. 계획의 소유자가 아닌 참가자만 사용할 수 있으며, 나가기 시 해당 계획의 참가자 목록에서 제거됩니다.")
    public RsData<Void> quitPlan(@PathVariable Long planId) {
        User user = rq.getUser();
        // TODO: 구현 필요
        return RsData.success("계획 나가기 성공", null);
    }
}