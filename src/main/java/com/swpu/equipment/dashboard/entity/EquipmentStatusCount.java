package com.swpu.equipment.dashboard.entity;

import lombok.Data;

@Data
public class EquipmentStatusCount {
    private String status;
    private String statusText;
    private Long count;
    private Double percentage;
}
