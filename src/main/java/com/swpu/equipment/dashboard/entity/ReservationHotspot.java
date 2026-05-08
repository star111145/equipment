package com.swpu.equipment.dashboard.entity;

import lombok.Data;

@Data
public class ReservationHotspot {
    private Long equipmentId;
    private String equipmentName;
    private String equipmentNumber;
    private String equipmentType;
    private Integer reservationCount;
}
