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
     * @EntityGraph 사용 시:
     * - Plan + participants + routes를 LEFT OUTER JOIN으로 한 번에 조회.
     * - 총 1번의 쿼리만 실행되어 N + 1 문제 방지 & 성능 향상.
     */
    @EntityGraph(attributePaths = {"participants", "routes"})
    Optional<Plan> findById(Long id);

    /**
     * @param userId
     * @return userId가 참가자로 연결된 모든 Plan을 조회
     */
    @EntityGraph(attributePaths = {"participants"})
    List<Plan> findDistinctByParticipants_UserId(Long userId);
}