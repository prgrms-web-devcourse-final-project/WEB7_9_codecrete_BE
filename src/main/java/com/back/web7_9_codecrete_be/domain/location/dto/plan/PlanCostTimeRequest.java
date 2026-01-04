package com.back.web7_9_codecrete_be.domain.location.dto.plan;

import com.back.web7_9_codecrete_be.domain.plans.entity.Schedule;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlanCostTimeRequest {

//    private Schedule.TransportType scheduleType;
    private double startX;
    private double startY;
    private double endX;
    private double endY;
    private Schedule.TransportType transportType;
}
