package com.swpu.equipment.dashboard.entity;

import lombok.Data;

@Data
public class EquipmentUsageTrend {
    private String date;
    private Long borrowCount;
    private Long reserveCount;
    private Long returnCount;
    private Long repairCount;
}
