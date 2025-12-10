package com.back.web7_9_codecrete_be.domain.plans.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanController {
    // GET /api/v1/plans/list/ - 계획 목록 조회
    // GET /api/v1/plans/{planID} - 계획 상세 조회
    // POST /api/v1/plans/{concertID} - 계획 생성
    // PATCH /api/v1/plans/update/{planID} - 계획 수정
    // DELETE /api/v1/plans/delete/{planID} - 계획 삭제
    // POST /api/v1/plans/invite/{planID} - 계획 공유 초대
    // POST /api/v1/plans/accept/{planID} - 계획 공유 수락
    // POST /api/v1/plans/deny/{planID} - 계획 공유 거절
    // DELETE /api/v1/plans/kick/{planID} - 계획 공유 인원 추방
    // DELETE /api/v1/plans/quit/{planID} - 계획 공유 나가기
}

