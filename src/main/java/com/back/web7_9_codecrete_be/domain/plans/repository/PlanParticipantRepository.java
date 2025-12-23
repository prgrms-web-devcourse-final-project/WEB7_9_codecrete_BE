package com.back.web7_9_codecrete_be.domain.plans.repository;

import com.back.web7_9_codecrete_be.domain.plans.entity.PlanParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}