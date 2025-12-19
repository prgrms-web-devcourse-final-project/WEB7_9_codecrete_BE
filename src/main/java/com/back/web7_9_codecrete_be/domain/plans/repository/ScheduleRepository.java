package com.back.web7_9_codecrete_be.domain.plans.repository;

import com.back.web7_9_codecrete_be.domain.plans.entity.Schedule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /**
     * 특정 Plan의 모든 일정을 시작 시간 순서대로 조회
     * Plan, Concert, ConcertPlace까지 조인 (메인 이벤트의 Concert 정보 포함)
     */
    @EntityGraph(attributePaths = {"plan", "plan.concert", "plan.concert.concertPlace"})
    List<Schedule> findByPlan_PlanIdOrderByStartAtAsc(Long planId);

    /**
     * 특정 Plan과 ID로 일정 조회
     * Plan, Concert, ConcertPlace까지 조인 (메인 이벤트의 Concert 정보 포함)
     */
    @EntityGraph(attributePaths = {"plan", "plan.concert", "plan.concert.concertPlace"})
    Optional<Schedule> findByScheduleIdAndPlan_PlanId(Long scheduleId, Long planId);

}