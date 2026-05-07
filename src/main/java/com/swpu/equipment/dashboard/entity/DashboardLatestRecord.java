package com.swpu.equipment.dashboard.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DashboardLatestRecord {
    private Long id;
    private String title;
    private String type;
    private LocalDateTime createdAt;
    private String status;
    private String userName;
    private Long userId;
    private String equipmentName;
}
