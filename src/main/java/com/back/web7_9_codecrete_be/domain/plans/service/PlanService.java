package com.back.web7_9_codecrete_be.domain.plans.service;

import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import com.back.web7_9_codecrete_be.domain.concerts.repository.ConcertRepository;
import com.back.web7_9_codecrete_be.domain.plans.dto.request.PlanAddRequest;
import com.back.web7_9_codecrete_be.domain.plans.dto.request.PlanUpdateRequest;
import com.back.web7_9_codecrete_be.domain.plans.dto.response.PlanDetailResponse;
import com.back.web7_9_codecrete_be.domain.plans.dto.response.PlanListResponse;
import com.back.web7_9_codecrete_be.domain.plans.dto.response.PlanResponse;
import com.back.web7_9_codecrete_be.domain.plans.dto.response.PlanDeleteResponse;
import com.back.web7_9_codecrete_be.domain.plans.entity.Plan;
import com.back.web7_9_codecrete_be.domain.plans.repository.PlanRepository;
import com.back.web7_9_codecrete_be.global.error.code.PlanErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanService {
    
    private final PlanRepository planRepository;
    private final ConcertRepository concertRepository;

    /**
     * 계획 생성
     *
     * @param concertId 콘서트 ID
     * @param request 계획 생성 요청 DTO
     * @return 생성된 계획 정보
     */
    @Transactional
    public PlanResponse createPlan(Long concertId, PlanAddRequest request) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new BusinessException(PlanErrorCode.PLAN_NOT_FOUND));

        Plan plan = Plan.builder()
                .concert(concert)
                .title(request.getTitle())
                .date(request.getDate())
                .build();
        
        plan = planRepository.save(plan);
        
        return PlanResponse.builder()
                .id(plan.getId())
                .concertId(plan.getConcert().getConcertId())
                .title(plan.getTitle())
                .date(plan.getDate())
                .createdDate(plan.getCreatedDate())
                .modifiedDate(plan.getModifiedDate())
                .build();
    }


    /**
     * 계획 목록 조회
     *
     * @return 계획 목록
     */
    public List<PlanListResponse> getPlanList(Long userId) {
        List<Plan> plans = planRepository.findDistinctByParticipants_UserId(userId);
        
        return plans.stream()
                .map(plan -> PlanListResponse.builder()
                        .id(plan.getId())
                        .concertId(plan.getConcert().getConcertId())
                        .title(plan.getTitle())
                        .date(plan.getDate())
                        .createdDate(plan.getCreatedDate())
                        .modifiedDate(plan.getModifiedDate())
                        .build())
                .collect(Collectors.toList());
    }


    /**
     * 계획 상세 조회
     * 
     * @param planId 계획 ID
     * @return 계획 상세 정보 (참가자, 경로 포함)
     * @throws BusinessException 계획을 찾을 수 없는 경우
     */
    public PlanDetailResponse getPlanDetail(Long planId, Long userId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(PlanErrorCode.PLAN_NOT_FOUND));

        boolean isParticipant = plan.getParticipants().stream()
                .anyMatch(participant -> participant.getUserId().equals(userId));

        if (!isParticipant) {
            throw new BusinessException(PlanErrorCode.PLAN_FORBIDDEN);
        }
        
        // @EntityGraph로 이미 로드된 participants와 routes를 DTO로 변환.
        List<PlanDetailResponse.ParticipantInfo> participants = plan.getParticipants().stream()
                .map(participant -> PlanDetailResponse.ParticipantInfo.builder()
                        .id(participant.getId())
                        .userId(participant.getUserId())
                        .inviteStatus(participant.getInviteStatus())
                        .role(participant.getRole())
                        .build())
                .collect(Collectors.toList());
        
        List<PlanDetailResponse.RouteInfo> routes = plan.getRoutes().stream()
                .map(route -> PlanDetailResponse.RouteInfo.builder()
                        .routeId(route.getRouteId())
                        .startPlaceLat(route.getStartPlaceLat())
                        .startPlaceLon(route.getStartPlaceLon())
                        .endPlaceLat(route.getEndPlaceLat())
                        .endPlaceLon(route.getEndPlaceLon())
                        .distance(route.getDistance())
                        .duration(route.getDuration())
                        .routeType(route.getRouteType())
                        .build())
                .collect(Collectors.toList());
        
        return PlanDetailResponse.builder()
                .id(plan.getId())
                .concertId(plan.getConcert().getConcertId())
                .title(plan.getTitle())
                .date(plan.getDate())
                .createdDate(plan.getCreatedDate())
                .modifiedDate(plan.getModifiedDate())
                .participants(participants)
                .routes(routes)
                .build();
    }

    /**
     * 계획 수정
     * 
     * @param planId 계획 ID
     * @param request 계획 수정 요청 DTO
     * @return 수정된 계획 정보
     * @throws BusinessException 계획을 찾을 수 없는 경우
     */
    @Transactional
    public PlanResponse updatePlan(Long planId, PlanUpdateRequest request) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(PlanErrorCode.PLAN_NOT_FOUND));
        
        // 엔티티의 비즈니스 로직을 통해 수정
        plan.update(request.getTitle(), request.getDate());
        
        return PlanResponse.builder()
                .id(plan.getId())
                .concertId(plan.getConcert().getConcertId())
                .title(plan.getTitle())
                .date(plan.getDate())
                .createdDate(plan.getCreatedDate())
                .modifiedDate(plan.getModifiedDate())
                .build();
    }

    /**
     * 계획 삭제
     * 
     * @param planId 계획 ID
     * @return 삭제된 계획 ID
     * @throws BusinessException 계획을 찾을 수 없는 경우
     */
    @Transactional
    public PlanDeleteResponse deletePlan(Long planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(PlanErrorCode.PLAN_NOT_FOUND));
        
        // Plan 삭제 시 cascade 설정으로 인해 participants와 routes도 함께 삭제.
        planRepository.delete(plan);
        
        return PlanDeleteResponse.builder()
                .planId(planId)
                .build();
    }


    // 계획 공유 초대
    // 계획 공유 수락
    // 계획 공유 거절
    // 계획 공유 인원 추방
    // 계획 공유 나가기
}