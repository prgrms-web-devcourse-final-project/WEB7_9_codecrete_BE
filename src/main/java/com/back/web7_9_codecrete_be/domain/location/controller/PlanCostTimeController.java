package com.back.web7_9_codecrete_be.domain.location.controller;

import com.back.web7_9_codecrete_be.domain.location.dto.plan.PlanCostTimeRequest;
import com.back.web7_9_codecrete_be.domain.location.dto.plan.PlanCostTimeResponse;
import com.back.web7_9_codecrete_be.domain.location.service.PlanCostTimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/location/plan")
public class PlanCostTimeController {

    private final PlanCostTimeService planCostTimeService;

    @PostMapping("/costtime")
    public PlanCostTimeResponse getCostTime(@RequestBody PlanCostTimeRequest request) {
        return planCostTimeService.getCostTime(request);
    }
}