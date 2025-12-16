package com.back.web7_9_codecrete_be.domain.plans.repository;

import com.back.web7_9_codecrete_be.domain.plans.entity.Plan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

    /**
     * - Plan 상세 조회 및 권한 체크 시
     * @param id Plan ID
     * @return Plan 엔티티 (concert, participants, schedules 포함)
     * @EntityGraph:
     * - Plan + concert + participants + schedules를 LEFT OUTER JOIN으로 한 번에 조회
     * - 총 1번의 쿼리만 실행되어 N + 1 문제 방지 & 성능 향상
     */
    @EntityGraph(attributePaths = {"concert", "participants", "schedules"})
    Optional<Plan> findById(Long id);

    /**
     * - 특정 사용자가 참가자로 포함된 모든 Plan 조회
     * @param userId 참가자로 포함된 사용자 ID
     * @return 해당 사용자가 참가자인 모든 Plan 목록 (concert, participants, schedules 포함)
     */
    @EntityGraph(attributePaths = {"concert", "participants", "schedules"})
    List<Plan> findDistinctByParticipants_User_Id(Long userId);

    /**
     * - 사용자가 소유자인 Plan들을 조회
     * @param userId Plan을 생성한 사용자 ID
     * @return 해당 사용자가 생성한 모든 Plan 목록 (schedules만 포함)
     */
    @EntityGraph(attributePaths = {"schedules"})
    List<Plan> findByUser_Id(Long userId);
}