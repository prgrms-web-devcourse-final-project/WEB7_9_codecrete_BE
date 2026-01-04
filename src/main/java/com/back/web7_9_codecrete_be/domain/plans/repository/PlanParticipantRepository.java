package com.back.web7_9_codecrete_be.domain.plans.repository;

import com.back.web7_9_codecrete_be.domain.plans.entity.PlanParticipant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanParticipantRepository extends JpaRepository<PlanParticipant, Long> {

    /**
     * 특정 사용자와 플랜의 조합으로 참가자 존재 여부 확인
     * @param userId 사용자 ID
     * @param planId 플랜 ID
     * @return 존재 여부
     */
    boolean existsByUser_IdAndPlan_PlanId(Long userId, Long planId);

    /**
     * 특정 사용자와 플랜의 조합으로 참가자 조회
     * @param userId 사용자 ID
     * @param planId 플랜 ID
     * @return PlanParticipant
     */
    Optional<PlanParticipant> findByUser_IdAndPlan_PlanId(Long userId, Long planId);

    /**
     * 특정 플랜의 모든 참가자 조회 (User 정보 포함)
     * @EntityGraph를 사용하여 User 정보를 함께 조회하여 N+1 문제 방지
     * @param planId 플랜 ID
     * @return 참가자 목록
     */
    @EntityGraph(attributePaths = {"user"})
    List<PlanParticipant> findByPlan_PlanId(Long planId);
}