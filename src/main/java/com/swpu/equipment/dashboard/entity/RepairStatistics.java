package com.swpu.equipment.dashboard.entity;

import lombok.Data;

@Data
public class RepairStatistics {
    private Integer totalRepairs;
    private Integer pendingRepairs;
    private Integer inProgressRepairs;
    private Integer completedRepairs;
    private Integer rejectedRepairs;
    private Double averageProcessingTime;
}
