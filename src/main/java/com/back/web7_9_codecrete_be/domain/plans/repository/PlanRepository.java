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
     * @return Plan 엔티티 (concert, participants 포함)
     * schedules는 @BatchSize로 배치 로드됨 (MultipleBagFetchException 방지)
     */
    @EntityGraph(attributePaths = {"concert", "participants"})
    Optional<Plan> findById(Long id);

    /**
     * - 특정 사용자가 참가자로 포함된 모든 Plan 조회
     * @param userId 참가자로 포함된 사용자 ID
     * @return 해당 사용자가 참가자인 모든 Plan 목록 (concert, participants 포함)
     * schedules는 @BatchSize로 배치 로드됨 (MultipleBagFetchException 방지)
     */
    @EntityGraph(attributePaths = {"concert", "participants"})
    List<Plan> findDistinctByParticipants_User_Id(Long userId);

    /**
     * - 사용자가 소유자인 Plan들을 조회
     * @param userId Plan을 생성한 사용자 ID
     * @return 해당 사용자가 생성한 모든 Plan 목록 (schedules만 포함)
     */
    @EntityGraph(attributePaths = {"schedules"})
    List<Plan> findByUser_Id(Long userId);

    /**
     * - shareToken으로 Plan 조회
     * @param shareToken 공유 토큰
     * @return 해당 토큰을 가진 Plan 엔티티 (concert, participants 포함)
     * schedules는 @BatchSize로 배치 로드됨 (MultipleBagFetchException 방지)
     */
    @EntityGraph(attributePaths = {"concert", "participants"})
    Optional<Plan> findByShareToken(String shareToken);
}