package com.swpu.equipment.dashboard.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DashboardPendingAction {
    private Long id;
    private String title;
    private String type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String status;
    private String userName;
    private String equipmentName;
}
