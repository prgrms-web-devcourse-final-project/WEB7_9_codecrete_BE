package com.back.web7_9_codecrete_be.domain.plans.repository;

import com.back.web7_9_codecrete_be.domain.plans.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /**
     * 특정 Plan의 모든 일정을 시작 시간 순서대로 조회
     */
    List<Schedule> findByPlan_PlanIdOrderByStartAtAsc(Long planId);

    /**
     * 특정 Plan에 속한 Schedule의 개수 조회 (다음 순서 계산용)
     */
    long countByPlan_PlanId(Long planId);

    /**
     * 특정 Plan과 ID로 일정 조회
     */
    Optional<Schedule> findByScheduleIdAndPlan_PlanId(Long scheduleId, Long planId);
}
