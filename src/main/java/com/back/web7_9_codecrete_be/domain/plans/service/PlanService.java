package com.back.web7_9_codecrete_be.domain.plans.service;

import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import com.back.web7_9_codecrete_be.domain.concerts.repository.ConcertRepository;
import com.back.web7_9_codecrete_be.domain.plans.dto.request.*;
import com.back.web7_9_codecrete_be.domain.plans.dto.response.*;
import com.back.web7_9_codecrete_be.domain.plans.entity.Plan;
import com.back.web7_9_codecrete_be.domain.plans.entity.PlanParticipant;
import com.back.web7_9_codecrete_be.domain.plans.entity.Schedule;
import com.back.web7_9_codecrete_be.domain.plans.repository.PlanParticipantRepository;
import com.back.web7_9_codecrete_be.domain.plans.repository.PlanRepository;
import com.back.web7_9_codecrete_be.domain.plans.repository.ScheduleRepository;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.global.error.code.PlanErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanService {
    
    private final PlanRepository planRepository;
    private final ConcertRepository concertRepository;
    private final ScheduleRepository scheduleRepository;
    private final PlanParticipantRepository planParticipantRepository;

    /**
     * 계획 생성
     * @param user 현재 로그인한 사용자
     * @param request 계획 생성 요청 DTO
     * @return 생성된 계획 정보
     */
    @Transactional
    public PlanResponse createPlan(User user, PlanAddRequest request) {
        Concert concert = concertRepository.findById(request.getConcertId())
                .orElseThrow(() -> new BusinessException(PlanErrorCode.CONCERT_NOT_FOUND));

        Plan plan = Plan.builder()
                .concert(concert)
                .user(user)
                .title(request.getTitle())
                .planDate(request.getPlanDate())
                .build();

        PlanParticipant owner = PlanParticipant.builder()
                .user(user)
                .plan(plan)
                .inviteStatus(PlanParticipant.InviteStatus.JOINED)
                .role(PlanParticipant.ParticipantRole.OWNER)
                .build();
        
        plan.addParticipant(owner);
        
        // 콘서트(메인 이벤트) 일정 자동 생성
        Schedule mainEventSchedule = Schedule.builder()
                .plan(plan)
                .scheduleType(Schedule.ScheduleType.ACTIVITY)
                .title(concert.getName())
                .startAt(LocalTime.of(0, 0)) // 기본값: 00:00 (사용자가 수정 필요)
                .duration(0) // 기본값: 0분 (사용자가 수정 필요)
                .location(concert.getConcertPlace() != null ? concert.getConcertPlace().getPlaceName() : "공연 장소")
                .locationLat(concert.getConcertPlace() != null ? concert.getConcertPlace().getLat() : null)
                .locationLon(concert.getConcertPlace() != null ? concert.getConcertPlace().getLon() : null)
                .estimatedCost(concert.getMinPrice())
                .details("공연 관람")
                .isMainEvent(true)
                .build();
        
        plan.addSchedule(mainEventSchedule);
        planRepository.save(plan);
        
        return toPlanResponse(plan);
    }


    /**
     * 계획 목록 조회
     * 소유자(OWNER)와 참가자(EDITOR/VIEWER)인 Plan을 모두 조회
     *
     * @param user 현재 로그인한 사용자
     * @return 계획 목록
     */
    public List<PlanListResponse> getPlanList(User user) {
        Long userId = user.getId();
        List<Plan> plans = planRepository.findDistinctByParticipants_User_Id(userId);
        
        return plans.stream()
                .map(plan -> {
                    // 일정 개수 및 총 소요 시간 계산
                    int scheduleCount = plan.getSchedules().size();
                    int totalDuration = calculateTotalDuration(plan.getSchedules());
                    
                    return PlanListResponse.builder()
                            .id(plan.getPlanId())
                            .concertId(plan.getConcert().getConcertId())
                            .createdBy(plan.getUserId())
                            .title(plan.getTitle())
                            .planDate(plan.getPlanDate())
                            .createdDate(plan.getCreatedDate())
                            .modifiedDate(plan.getModifiedDate())
                            .scheduleCount(scheduleCount)
                            .totalDuration(totalDuration)
                            .build();
                })
                .collect(Collectors.toList());
    }


    /**
     * 계획 상세 조회
     * 
     * @param planId 계획 ID
     * @param user 현재 로그인한 사용자
     * @return 계획 상세 정보 (참가자, 경로 포함)
     * @throws BusinessException 계획을 찾을 수 없는 경우
     */
    public PlanDetailResponse getPlanDetail(Long planId, User user) {
        Plan plan = findPlanWithParticipantCheck(planId, user);
        return buildPlanDetailResponse(plan);
    }

    /**
     * Plan 엔티티를 PlanDetailResponse로 변환 (공통 메서드)
     * 
     * @param plan Plan 엔티티
     * @return 계획 상세 정보
     */
    private PlanDetailResponse buildPlanDetailResponse(Plan plan) {
        List<PlanDetailResponse.ParticipantInfo> participants = plan.getParticipants().stream()
                .map(participant -> PlanDetailResponse.ParticipantInfo.builder()
                        .id(participant.getParticipantId())
                        .userId(participant.getUserId())
                        .inviteStatus(participant.getInviteStatus())
                        .role(participant.getRole())
                        .build())
                .collect(Collectors.toList());
        
        // 타임라인 형태로 일정 정렬 (startAt 기준) - 메인 이벤트와 일반 일정 모두 포함
        // Concert 정보까지 포함하여 조회 (메인 이벤트의 Concert 정보 포함)
        List<Schedule> sortedSchedules = scheduleRepository
                .findByPlan_PlanIdOrderByStartAtAsc(plan.getPlanId());
        
        List<PlanDetailResponse.ScheduleInfo> schedules = sortedSchedules.stream()
                .map(item -> {
                    PlanDetailResponse.ScheduleInfo.ScheduleInfoBuilder builder = PlanDetailResponse.ScheduleInfo.builder()
                            .id(item.getScheduleId())
                            .scheduleType(item.getScheduleType())
                            .title(item.getTitle())
                            .startAt(item.getStartAt())
                            .duration(item.getDuration())
                            .location(item.getLocation())
                            .locationLat(item.getLocationLat())
                            .locationLon(item.getLocationLon())
                            .estimatedCost(item.getEstimatedCost())
                            .details(item.getDetails())
                            .startPlaceLat(item.getStartPlaceLat())
                            .startPlaceLon(item.getStartPlaceLon())
                            .endPlaceLat(item.getEndPlaceLat())
                            .endPlaceLon(item.getEndPlaceLon())
                            .distance(item.getDistance())
                            .transportType(item.getTransportType())
                            .isMainEvent(item.getIsMainEvent())
                            .createdDate(item.getCreatedDate())
                            .modifiedDate(item.getModifiedDate());

                    // 메인 이벤트인 경우 공연 정보 추가
                    if (isMainEvent(item)) {
                        addConcertInfoToScheduleInfoBuilder(builder, item);
                    }

                    return builder.build();
                })
                .collect(Collectors.toList());
        
        // 총 소요 시간 계산
        Integer totalDuration = calculateTotalDuration(sortedSchedules);
        
        return PlanDetailResponse.builder()
                .id(plan.getPlanId())
                .concertId(plan.getConcert().getConcertId())
                .createdBy(plan.getUserId())
                .title(plan.getTitle())
                .planDate(plan.getPlanDate())
                .createdDate(plan.getCreatedDate())
                .modifiedDate(plan.getModifiedDate())
                .participants(participants)
                .schedules(schedules)
                .totalDuration(totalDuration)
                .build();
    }


    /**
     * 계획 수정
     * 
     * @param planId 계획 ID
     * @param user 현재 로그인한 사용자
     * @param request 계획 수정 요청 DTO
     * @return 수정된 계획 정보
     * @throws BusinessException 계획을 찾을 수 없는 경우
     */
    @Transactional
    public PlanResponse updatePlan(Long planId, User user, PlanUpdateRequest request) {
        Plan plan = findPlanWithEditPermissionCheck(planId, user);
        
        // 부분 업데이트 (null인 경우 기존 값 유지)
        String title = request.getTitle() != null ? request.getTitle() : plan.getTitle();
        LocalDate planDate = request.getPlanDate() != null ? request.getPlanDate() : plan.getPlanDate();
        
        // 엔티티의 로직을 통해 수정
        plan.update(title, planDate);
        
        return toPlanResponse(plan);
    }


    /**
     * 계획 삭제
     * 
     * @param planId 계획 ID
     * @param user 현재 로그인한 사용자
     * @return 삭제된 계획 ID
     * @throws BusinessException 계획을 찾을 수 없는 경우
     */
    @Transactional
    public PlanDeleteResponse deletePlan(Long planId, User user) {
        Plan plan = findPlanWithOwnerCheck(planId, user);
        Long deletedPlanId = plan.getPlanId();
        
        // Plan 삭제 시 cascade 설정으로 인해 participants와 schedules도 함께 삭제.
        planRepository.delete(plan);
        
        return PlanDeleteResponse.builder()
                .planId(deletedPlanId)
                .build();
    }


    /**
     * 일정 추가
     *
     * @param planId 계획 ID
     * @param user 현재 로그인한 사용자
     * @param request 일정 추가 요청 DTO
     * @return 생성된 일정 정보
     */
    @Transactional
    public ScheduleResponse addSchedule(Long planId, User user, ScheduleAddRequest request) {
        Plan plan = findPlanWithEditPermissionCheck(planId, user);

        // TRANSPORT 타입일 때 locationLat/Lon은 사용하지 않음 (endPlaceLat/Lon 사용)
        if (request.getScheduleType() == Schedule.ScheduleType.TRANSPORT) {
            if (request.getLocationLat() != null || request.getLocationLon() != null) {
                throw new BusinessException(PlanErrorCode.SCHEDULE_INVALID_LOCATION_FOR_TRANSPORT);
            }
            // TRANSPORT 타입인 경우 필수 필드 검증
            if (request.getStartPlaceLat() == null || request.getStartPlaceLon() == null ||
                request.getEndPlaceLat() == null || request.getEndPlaceLon() == null ||
                request.getDistance() == null || request.getTransportType() == null) {
                throw new BusinessException(PlanErrorCode.SCHEDULE_INVALID_TRANSPORT_FIELDS);
            }
        }

        Schedule schedule = Schedule.builder()
                .plan(plan)
                .scheduleType(request.getScheduleType())
                .title(request.getTitle())
                .startAt(request.getStartAt())
                .duration(request.getDuration())
                .location(request.getLocation())
                .locationLat(request.getLocationLat())
                .locationLon(request.getLocationLon())
                .estimatedCost(request.getEstimatedCost())
                .details(request.getDetails())
                .startPlaceLat(request.getStartPlaceLat())
                .startPlaceLon(request.getStartPlaceLon())
                .endPlaceLat(request.getEndPlaceLat())
                .endPlaceLon(request.getEndPlaceLon())
                .distance(request.getDistance())
                .transportType(request.getTransportType())
                .build();

        plan.addSchedule(schedule);
        // cascade 설정으로 인해 plan 저장 시 schedule도 함께 저장됨
        planRepository.save(plan);

        // 저장 직후이므로 영속성 컨텍스트에 있는 schedule 사용 (재조회 불필요)
        return toScheduleResponse(schedule);
    }

    /**
     * 일정 목록 조회 (타임라인 형태)
     *
     * @param planId 계획 ID
     * @param user 현재 로그인한 사용자
     * @return 일정 목록 (순서대로 정렬)
     */
    public ScheduleListResponse getSchedules(Long planId, User user) {
        // 권한 체크 (참가자 여부 확인)
        findPlanWithParticipantCheck(planId, user);

        // 타임라인 형태로 일정 정렬 (startAt 기준) - 메인 이벤트와 일반 일정 모두 포함
        // Concert 정보까지 포함하여 조회 (메인 이벤트의 Concert 정보 포함)
        List<Schedule> schedules = scheduleRepository
                .findByPlan_PlanIdOrderByStartAtAsc(planId);

        List<ScheduleResponse> scheduleResponses = schedules.stream()
                .map(this::toScheduleResponse)
                .collect(Collectors.toList());

        // 총 소요 시간 계산
        Integer totalDuration = calculateTotalDuration(schedules);

        return ScheduleListResponse.builder()
                .planId(planId)
                .schedules(scheduleResponses)
                .totalDuration(totalDuration)
                .build();
    }

    /**
     * 일정 상세 조회
     *
     * @param planId 계획 ID
     * @param scheduleId 일정 ID
     * @param user 현재 로그인한 사용자
     * @return 일정 상세 정보
     */
    public ScheduleResponse getSchedule(Long planId, Long scheduleId, User user) {
        // 권한 체크 (참가자 여부 확인)
        findPlanWithParticipantCheck(planId, user);
        
        // 스케줄 조회 (Concert 정보 포함)
        Schedule schedule = scheduleRepository
                .findByScheduleIdAndPlan_PlanId(scheduleId, planId)
                .orElseThrow(() -> new BusinessException(PlanErrorCode.SCHEDULE_NOT_FOUND));

        return toScheduleResponse(schedule);
    }


    /**
     * 일정 수정
     *
     * @param planId 계획 ID
     * @param scheduleId 일정 ID
     * @param user 현재 로그인한 사용자
     * @param request 일정 수정 요청 DTO
     * @return 수정된 일정 정보
     */
    @Transactional
    public ScheduleResponse updateSchedule(Long planId, Long scheduleId, User user,
                                           ScheduleUpdateRequest request) {
        // 권한 체크 (수정 권한 확인: OWNER 또는 EDITOR)
        findPlanWithEditPermissionCheck(planId, user);
        
        Schedule schedule = scheduleRepository
                .findByScheduleIdAndPlan_PlanId(scheduleId, planId)
                .orElseThrow(() -> new BusinessException(PlanErrorCode.SCHEDULE_NOT_FOUND));

        // 부분 업데이트 지원: null인 경우 기존 값 유지
        Schedule.ScheduleType newScheduleType = request.getScheduleType() != null 
                ? request.getScheduleType() 
                : schedule.getScheduleType();
        
        // TRANSPORT 타입일 때 locationLat/Lon은 사용하지 않음 (endPlaceLat/Lon 사용)
        if (newScheduleType == Schedule.ScheduleType.TRANSPORT) {
            // locationLat/Lon이 제공된 경우 에러
            if (request.getLocationLat() != null || request.getLocationLon() != null) {
                throw new BusinessException(PlanErrorCode.SCHEDULE_INVALID_LOCATION_FOR_TRANSPORT);
            }
        }

        // 일반 일정일 때 위도/경도는 쌍으로만 허용 (단독 입력 방지)
        if (newScheduleType != Schedule.ScheduleType.TRANSPORT) {
            boolean isLatProvided = request.getLocationLat() != null;
            boolean isLonProvided = request.getLocationLon() != null;
            if (isLatProvided ^ isLonProvided) {
                throw new BusinessException(PlanErrorCode.SCHEDULE_INVALID_LOCATION_COORDINATES);
            }
        }
        
        // TRANSPORT 타입인 경우 필수 필드 검증
        if (newScheduleType == Schedule.ScheduleType.TRANSPORT) {
            Double startPlaceLat = request.getStartPlaceLat() != null 
                    ? request.getStartPlaceLat() 
                    : schedule.getStartPlaceLat();
            Double startPlaceLon = request.getStartPlaceLon() != null 
                    ? request.getStartPlaceLon() 
                    : schedule.getStartPlaceLon();
            Double endPlaceLat = request.getEndPlaceLat() != null 
                    ? request.getEndPlaceLat() 
                    : schedule.getEndPlaceLat();
            Double endPlaceLon = request.getEndPlaceLon() != null 
                    ? request.getEndPlaceLon() 
                    : schedule.getEndPlaceLon();
            Integer distance = request.getDistance() != null 
                    ? request.getDistance()
                    : schedule.getDistance();
            Schedule.TransportType transportType = request.getTransportType() != null 
                    ? request.getTransportType() 
                    : schedule.getTransportType();
            
            if (startPlaceLat == null || startPlaceLon == null || 
                endPlaceLat == null || endPlaceLon == null || 
                distance == null || transportType == null) {
                throw new BusinessException(PlanErrorCode.SCHEDULE_INVALID_TRANSPORT_FIELDS);
            }
        }

        schedule.update(
                newScheduleType,
                request.getTitle() != null ? request.getTitle() : schedule.getTitle(),
                request.getStartAt() != null ? request.getStartAt() : schedule.getStartAt(),
                request.getDuration() != null ? request.getDuration() : schedule.getDuration(),
                request.getLocation() != null ? request.getLocation() : schedule.getLocation(),
                request.getLocationLat() != null ? request.getLocationLat() : schedule.getLocationLat(),
                request.getLocationLon() != null ? request.getLocationLon() : schedule.getLocationLon(),
                request.getEstimatedCost() != null ? request.getEstimatedCost() : schedule.getEstimatedCost(),
                request.getDetails() != null ? request.getDetails() : schedule.getDetails(),
                request.getStartPlaceLat() != null ? request.getStartPlaceLat() : schedule.getStartPlaceLat(),
                request.getStartPlaceLon() != null ? request.getStartPlaceLon() : schedule.getStartPlaceLon(),
                request.getEndPlaceLat() != null ? request.getEndPlaceLat() : schedule.getEndPlaceLat(),
                request.getEndPlaceLon() != null ? request.getEndPlaceLon() : schedule.getEndPlaceLon(),
                request.getDistance() != null ? request.getDistance() : schedule.getDistance(),
                request.getTransportType() != null ? request.getTransportType() : schedule.getTransportType()
        );

        // 수정 직후이므로 영속성 컨텍스트에 있는 schedule 사용 (재조회 불필요)
        return toScheduleResponse(schedule);
    }


    /**
     * 일정 삭제
     *
     * @param planId 계획 ID
     * @param scheduleId 일정 ID
     * @param user 현재 로그인한 사용자
     * @return 삭제된 일정 ID
     */
    @Transactional
    public ScheduleDeleteResponse deleteSchedule(Long planId, Long scheduleId, User user) {
        // 권한 체크 (수정 권한 확인: OWNER 또는 EDITOR)
        findPlanWithEditPermissionCheck(planId, user);
        
        Schedule schedule = scheduleRepository
                .findByScheduleIdAndPlan_PlanId(scheduleId, planId)
                .orElseThrow(() -> new BusinessException(PlanErrorCode.SCHEDULE_NOT_FOUND));

        // 메인 이벤트(콘서트) 일정은 삭제 불가
        if (isMainEvent(schedule)) {
            throw new BusinessException(PlanErrorCode.SCHEDULE_MAIN_EVENT_NOT_DELETABLE);
        }

        scheduleRepository.delete(schedule);
        
        return ScheduleDeleteResponse.builder()
                .scheduleId(scheduleId)
                .build();
    }


    /**
     * Plan을 조회하고 참가자 권한을 체크하는 메서드
     *
     * @param planId 계획 ID
     * @param user 현재 로그인한 사용자
     * @return Plan 엔티티
     * @throws BusinessException 계획을 찾을 수 없거나 권한이 없는 경우
     */
    private Plan findPlanWithParticipantCheck(Long planId, User user) {
        Long userId = user.getId();
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(PlanErrorCode.PLAN_NOT_FOUND));

        // 소유자는 항상 참가자이므로 바로 통과
        if (plan.getUserId().equals(userId)) {
            return plan;
        }

        // 소유자가 아닌 경우 PlanParticipant에서 참가 여부 확인
        plan.getParticipants().size();

        boolean isParticipant = plan.getParticipants().stream()
                .anyMatch(participant -> participant.getUserId().equals(userId));

        if (!isParticipant) {
            throw new BusinessException(PlanErrorCode.PLAN_FORBIDDEN);
        }

        return plan;
    }

    /**
     * Plan을 조회하고 소유자 권한을 체크
     * 소유자만 삭제 가능
     *
     * @param planId 계획 ID
     * @param user 현재 로그인한 사용자
     * @return Plan 엔티티
     * @throws BusinessException 계획을 찾을 수 없거나 소유자가 아닌 경우
     */
    private Plan findPlanWithOwnerCheck(Long planId, User user) {
        Long userId = user.getId();
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(PlanErrorCode.PLAN_NOT_FOUND));

        // 소유자만 삭제 가능
        if (!plan.getUserId().equals(userId)) {
            throw new BusinessException(PlanErrorCode.PLAN_UNAUTHORIZED);
        }

        return plan;
    }

    /**
     * Plan을 조회하고 수정/삭제 권한을 체크
     * OWNER 또는 EDITOR만 수정/삭제 가능
     *
     * @param planId 계획 ID
     * @param user 현재 로그인한 사용자
     * @return Plan 엔티티
     * @throws BusinessException 계획을 찾을 수 없거나 권한이 없는 경우
     */
    private Plan findPlanWithEditPermissionCheck(Long planId, User user) {
        Long userId = user.getId();
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(PlanErrorCode.PLAN_NOT_FOUND));

        // 소유자는 항상 OWNER 역할이므로 바로 통과
        if (plan.getUserId().equals(userId)) {
            return plan;
        }

        // 소유자가 아닌 경우 PlanParticipant에서 role 확인
        plan.getParticipants().size();
        
        PlanParticipant participant = plan.getParticipants().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(PlanErrorCode.PLAN_FORBIDDEN));

        // VIEWER는 수정/삭제 불가
        if (participant.getRole() == PlanParticipant.ParticipantRole.VIEWER) {
            throw new BusinessException(PlanErrorCode.PLAN_UNAUTHORIZED);
        }

        return plan;
    }


    /**
     * Plan을 PlanResponse로 변환
     */
    private PlanResponse toPlanResponse(Plan plan) {
        return PlanResponse.builder()
                .id(plan.getPlanId())
                .concertId(plan.getConcert().getConcertId())
                .createdBy(plan.getUserId())
                .title(plan.getTitle())
                .planDate(plan.getPlanDate())
                .createdDate(plan.getCreatedDate())
                .modifiedDate(plan.getModifiedDate())
                .build();
    }

    /**
     * 메인 이벤트 여부 확인
     */
    private boolean isMainEvent(Schedule schedule) {
        return schedule != null && schedule.getIsMainEvent() != null && schedule.getIsMainEvent();
    }

    /**
     * 총 소요 시간 계산
     */
    private Integer calculateTotalDuration(List<Schedule> schedules) {
        return schedules.stream()
                .filter(item -> item.getDuration() != null)
                .mapToInt(Schedule::getDuration)
                .sum();
    }

    /**
     * Concert 정보를 추출하여 반환
     */
    private Concert getConcertFromSchedule(Schedule schedule) {
        if (schedule != null && schedule.getPlan() != null) {
            return schedule.getPlan().getConcert();
        }
        return null;
    }

    /**
     * ScheduleInfo.Builder에 공연 정보 추가
     */
    private void addConcertInfoToScheduleInfoBuilder(
            PlanDetailResponse.ScheduleInfo.ScheduleInfoBuilder builder, Schedule schedule) {
        Concert concert = getConcertFromSchedule(schedule);
        if (concert != null) {
            builder.concertId(concert.getConcertId())
                    .concertName(concert.getName())
                    .concertPosterUrl(concert.getPosterUrl())
                    .concertPlaceName(concert.getConcertPlace() != null ? concert.getConcertPlace().getPlaceName() : null)
                    .concertMinPrice(concert.getMinPrice())
                    .concertMaxPrice(concert.getMaxPrice());
        }
    }

    /**
     * ScheduleResponse.Builder에 공연 정보 추가
     */
    private void addConcertInfoToBuilder(ScheduleResponse.ScheduleResponseBuilder builder, Schedule schedule) {
        Concert concert = getConcertFromSchedule(schedule);
        if (concert != null) {
            builder.concertId(concert.getConcertId())
                    .concertName(concert.getName())
                    .concertPosterUrl(concert.getPosterUrl())
                    .concertPlaceName(concert.getConcertPlace() != null ? concert.getConcertPlace().getPlaceName() : null)
                    .concertMinPrice(concert.getMinPrice())
                    .concertMaxPrice(concert.getMaxPrice());
        }
    }

    private ScheduleResponse toScheduleResponse(Schedule schedule) {
        ScheduleResponse.ScheduleResponseBuilder builder = ScheduleResponse.builder()
                .id(schedule.getScheduleId())
                .scheduleType(schedule.getScheduleType())
                .title(schedule.getTitle())
                .startAt(schedule.getStartAt())
                .duration(schedule.getDuration())
                .location(schedule.getLocation())
                .locationLat(schedule.getLocationLat())
                .locationLon(schedule.getLocationLon())
                .estimatedCost(schedule.getEstimatedCost())
                .details(schedule.getDetails())
                .startPlaceLat(schedule.getStartPlaceLat())
                .startPlaceLon(schedule.getStartPlaceLon())
                .endPlaceLat(schedule.getEndPlaceLat())
                .endPlaceLon(schedule.getEndPlaceLon())
                .distance(schedule.getDistance())
                .transportType(schedule.getTransportType())
                .isMainEvent(schedule.getIsMainEvent())
                .createdDate(schedule.getCreatedDate())
                .modifiedDate(schedule.getModifiedDate());

        // 메인 이벤트인 경우 공연 정보 추가
        if (isMainEvent(schedule)) {
            addConcertInfoToBuilder(builder, schedule);
        }

        return builder.build();
    }


    /**
     * 참가자 역할 수정
     *
     * @param planId 계획 ID
     * @param participantId 참가자 ID
     * @param user 현재 로그인한 사용자 (권한 체크용)
     * @param request 역할 수정 요청 DTO
     * @throws BusinessException 계획을 찾을 수 없거나 권한이 없는 경우
     */
    @Transactional
    public void updateParticipantRole(Long planId, Long participantId, User user,
                                      PlanParticipantRoleUpdateRequest request) {
        // 권한 체크 (수정 권한 확인: OWNER 또는 EDITOR)
        findPlanWithEditPermissionCheck(planId, user);
        
        PlanParticipant participant = planParticipantRepository.findById(participantId)
                .orElseThrow(() -> new BusinessException(PlanErrorCode.PLAN_NOT_FOUND));
        
        // 참가자가 해당 Plan에 속해있는지 확인
        if (!participant.getPlan().getPlanId().equals(planId)) {
            throw new BusinessException(PlanErrorCode.PLAN_NOT_FOUND);
        }
        
        // OWNER 역할은 변경할 수 없음
        if (participant.getRole() == PlanParticipant.ParticipantRole.OWNER) {
            throw new BusinessException(PlanErrorCode.PLAN_UNAUTHORIZED);
        }
        
        // OWNER 역할로 변경할 수 없음 (소유자는 Plan의 userId로 결정됨)
        if (request.getRole() == PlanParticipant.ParticipantRole.OWNER) {
            throw new BusinessException(PlanErrorCode.PLAN_UNAUTHORIZED);
        }
        
        participant.updateRole(request.getRole());
    }

    /**
     * 공유 링크 생성 (UUID 기반 13자)
     *
     * @param planId 계획 ID
     * @param user 현재 로그인한 사용자 (권한 체크용)
     * @return 공유 링크 응답 DTO
     * @throws BusinessException 계획을 찾을 수 없거나 권한이 없는 경우
     */
    @Transactional
    public PlanShareLinkResponse generateShareLink(Long planId, User user) {
        // 권한 체크 (수정 권한 확인: OWNER 또는 EDITOR)
        Plan plan = findPlanWithEditPermissionCheck(planId, user);

        // shareToken이 없거나 만료되었으면 새로 생성, 유효하면 재사용
        if (plan.getShareToken() == null || plan.isShareTokenExpired()) {
            plan.generateShareToken();
            planRepository.save(plan);
        }

        return PlanShareLinkResponse.builder()
                .planId(plan.getPlanId())
                .shareToken(plan.getShareToken())
                .shareLink("/plans/share/" + plan.getShareToken())
                .build();
    }

    /**
     * 공유 링크 재생성 (재발급)
     * 이전 링크는 무효화되고 새로운 링크가 생성됩니다.
     * OWNER만 가능합니다.
     *
     * @param planId 계획 ID
     * @param user 현재 로그인한 사용자 (권한 체크용)
     * @return 공유 링크 응답 DTO
     * @throws BusinessException 계획을 찾을 수 없거나 OWNER가 아닌 경우
     */
    @Transactional
    public PlanShareLinkResponse regenerateShareLink(Long planId, User user) {
        // 권한 체크 (OWNER만 가능)
        Plan plan = findPlanWithOwnerCheck(planId, user);

        // 기존 토큰을 무효화하고 새로운 토큰 생성 (이전 링크 무효화)
        plan.generateShareToken();
        planRepository.save(plan);

        return PlanShareLinkResponse.builder()
                .planId(plan.getPlanId())
                .shareToken(plan.getShareToken())
                .shareLink("/plans/share/" + plan.getShareToken())
                .build();
    }

    /**
     * 공유 링크로 플랜 조회 (참가자 생성 없이 조회만)
     *
     * @param shareToken 공유 토큰 (UUID 기반 13자)
     * @param user 현재 로그인한 사용자
     * @return 플랜 상세 정보
     * @throws BusinessException 공유 링크가 유효하지 않은 경우
     */
    public PlanDetailResponse getPlanByShareToken(String shareToken, User user) {
        // shareToken으로 Plan 찾기
        Plan plan = planRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new BusinessException(PlanErrorCode.INVALID_SHARE_TOKEN));

        // 만료 시간 검증
        if (plan.isShareTokenExpired()) {
            throw new BusinessException(PlanErrorCode.SHARE_TOKEN_EXPIRED);
        }

        // 자기 자신의 플랜은 조회 불가
        if (plan.getUserId().equals(user.getId())) {
            throw new BusinessException(PlanErrorCode.USER_ALREADY_PARTICIPANT);
        }

        // 참가자 체크 없이 조회 가능 (공유 링크이므로)
        return buildPlanDetailResponse(plan);
    }

    /**
     * 공유 링크로 플랜 참가 수락 (참가자 생성)
     *
     * @param shareToken 공유 토큰 (UUID 기반 13자)
     * @param user 현재 로그인한 사용자
     * @return 플랜 상세 정보
     * @throws BusinessException 공유 링크가 유효하지 않은 경우, 이미 참가자인 경우
     */
    @Transactional
    public PlanDetailResponse acceptPlanInvitation(String shareToken, User user) {
        // shareToken으로 Plan 찾기
        Plan plan = planRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new BusinessException(PlanErrorCode.INVALID_SHARE_TOKEN));

        // 만료 시간 검증
        if (plan.isShareTokenExpired()) {
            throw new BusinessException(PlanErrorCode.SHARE_TOKEN_EXPIRED);
        }

        // 자기 자신의 플랜은 참가할 수 없음
        if (plan.getUserId().equals(user.getId())) {
            throw new BusinessException(PlanErrorCode.USER_ALREADY_PARTICIPANT);
        }

        // DB 레벨에서 이미 참가자인지 확인 (유니크 제약조건 검증)
        boolean isAlreadyParticipant = planParticipantRepository.existsByUser_IdAndPlan_PlanId(
                user.getId(), plan.getPlanId());

        if (isAlreadyParticipant) {
            // 이미 참가자인 경우 상태를 ACCEPTED로 변경
            PlanParticipant participant = planParticipantRepository
                    .findByUser_IdAndPlan_PlanId(user.getId(), plan.getPlanId())
                    .orElseThrow(() -> new BusinessException(PlanErrorCode.PLAN_NOT_FOUND));

            participant.updateInviteStatus(PlanParticipant.InviteStatus.ACCEPTED);
            planParticipantRepository.save(participant);
        } else {
            // 새로운 참가자 추가 (기본 역할은 VIEWER, 상태는 ACCEPTED)
            PlanParticipant participant = PlanParticipant.builder()
                    .user(user)
                    .plan(plan)
                    .inviteStatus(PlanParticipant.InviteStatus.ACCEPTED)
                    .role(PlanParticipant.ParticipantRole.VIEWER)
                    .build();

            plan.addParticipant(participant);
            planRepository.save(plan);
        }

        return getPlanDetail(plan.getPlanId(), user);
    }

    /**
     * 공유 링크 삭제 (shareToken 제거)
     *
     * @param planId 계획 ID
     * @param user 현재 로그인한 사용자 (권한 체크용)
     * @throws BusinessException 계획을 찾을 수 없거나 권한이 없는 경우
     */
    @Transactional
    public void deleteShareLink(Long planId, User user) {
        // 권한 체크 (수정 권한 확인: OWNER 또는 EDITOR)
        Plan plan = findPlanWithEditPermissionCheck(planId, user);

        plan.clearShareToken();
        planRepository.save(plan);
    }

    /**
     * 초대 거절
     * 공유 링크를 통해 받은 초대를 거절합니다.
     *
     * @param shareToken 공유 토큰
     * @param user 현재 로그인한 사용자
     * @throws BusinessException 공유 링크가 유효하지 않거나 참가자가 아닌 경우
     */
    @Transactional
    public void declinePlanInvitation(String shareToken, User user) {
        // shareToken으로 Plan 찾기
        Plan plan = planRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new BusinessException(PlanErrorCode.INVALID_SHARE_TOKEN));

        // 만료 시간 검증
        if (plan.isShareTokenExpired()) {
            throw new BusinessException(PlanErrorCode.SHARE_TOKEN_EXPIRED);
        }

        // 자기 자신의 플랜은 거절할 수 없음
        if (plan.getUserId().equals(user.getId())) {
            throw new BusinessException(PlanErrorCode.USER_ALREADY_PARTICIPANT);
        }

        // 참가자 조회
        PlanParticipant participant = planParticipantRepository
                .findByUser_IdAndPlan_PlanId(user.getId(), plan.getPlanId())
                .orElseThrow(() -> new BusinessException(PlanErrorCode.PARTICIPANT_NOT_FOUND));

        // 상태를 DECLINED로 변경
        participant.updateInviteStatus(PlanParticipant.InviteStatus.DECLINED);
        planParticipantRepository.save(participant);
    }

    /**
     * 참가자 강퇴
     * OWNER 또는 EDITOR 권한이 있는 사용자가 다른 참가자를 강퇴합니다.
     *
     * @param planId 계획 ID
     * @param participantId 참가자 ID
     * @param user 현재 로그인한 사용자 (권한 체크용)
     * @throws BusinessException 계획을 찾을 수 없거나 권한이 없거나 소유자는 강퇴할 수 없는 경우
     */
    @Transactional
    public void kickParticipant(Long planId, Long participantId, User user) {
        // 권한 체크 (수정 권한 확인: OWNER 또는 EDITOR)
        Plan plan = findPlanWithEditPermissionCheck(planId, user);

        // 참가자 조회
        PlanParticipant participant = planParticipantRepository.findById(participantId)
                .orElseThrow(() -> new BusinessException(PlanErrorCode.PARTICIPANT_NOT_FOUND));

        // 참가자가 해당 Plan에 속해있는지 확인
        if (!participant.getPlan().getPlanId().equals(planId)) {
            throw new BusinessException(PlanErrorCode.PARTICIPANT_NOT_FOUND);
        }

        // 소유자는 강퇴할 수 없음
        if (participant.getRole() == PlanParticipant.ParticipantRole.OWNER ||
            plan.getUserId().equals(participant.getUserId())) {
            throw new BusinessException(PlanErrorCode.CANNOT_REMOVE_OWNER);
        }

        // 상태를 REMOVED로 변경
        participant.updateInviteStatus(PlanParticipant.InviteStatus.REMOVED);
        planParticipantRepository.save(participant);
    }

    /**
     * 자진 나가기
     * 참가자가 계획에서 스스로 나갑니다.
     *
     * @param planId 계획 ID
     * @param user 현재 로그인한 사용자
     * @throws BusinessException 계획을 찾을 수 없거나 소유자는 나갈 수 없는 경우
     */
    @Transactional
    public void quitPlan(Long planId, User user) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(PlanErrorCode.PLAN_NOT_FOUND));

        // 소유자는 나갈 수 없음
        if (plan.getUserId().equals(user.getId())) {
            throw new BusinessException(PlanErrorCode.CANNOT_LEAVE_OWNER);
        }

        // 참가자 조회
        PlanParticipant participant = planParticipantRepository
                .findByUser_IdAndPlan_PlanId(user.getId(), planId)
                .orElseThrow(() -> new BusinessException(PlanErrorCode.PARTICIPANT_NOT_FOUND));

        // 상태를 LEFT로 변경
        participant.updateInviteStatus(PlanParticipant.InviteStatus.LEFT);
        planParticipantRepository.save(participant);
    }

    /**
     * 참가자 목록 조회
     * 계획의 참가자 목록을 조회합니다. 상태별 필터링이 가능합니다.
     *
     * @param planId 계획 ID
     * @param user 현재 로그인한 사용자 (권한 체크용)
     * @param inviteStatus 필터링할 초대 상태 (선택적, null이면 모든 상태)
     * @return 참가자 목록
     * @throws BusinessException 계획을 찾을 수 없거나 권한이 없는 경우
     */
    public PlanParticipantListResponse getParticipantList(Long planId, User user, PlanParticipant.InviteStatus inviteStatus) {
        // 권한 체크 (참가자 여부 확인)
        findPlanWithParticipantCheck(planId, user);

        // 참가자 목록 조회 (User 정보 포함)
        List<PlanParticipant> participants = planParticipantRepository.findByPlan_PlanId(planId);

        // 상태별 필터링
        if (inviteStatus != null) {
            participants = participants.stream()
                    .filter(p -> p.getInviteStatus() == inviteStatus)
                    .collect(Collectors.toList());
        }

        // DTO 변환
        List<PlanParticipantListResponse.ParticipantDetailInfo> participantInfos = participants.stream()
                .map(participant -> PlanParticipantListResponse.ParticipantDetailInfo.builder()
                        .participantId(participant.getParticipantId())
                        .userId(participant.getUserId())
                        .nickname(participant.getUser().getNickname())
                        .email(participant.getUser().getEmail())
                        .profileImage(participant.getUser().getProfileImage())
                        .inviteStatus(participant.getInviteStatus())
                        .role(participant.getRole())
                        .build())
                .collect(Collectors.toList());

        return PlanParticipantListResponse.builder()
                .planId(planId)
                .participants(participantInfos)
                .build();
    }
}