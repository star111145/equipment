package com.swpu.equipment.dashboard.entity;

import lombok.Data;

@Data
public class DashboardStatistics {
    private Long totalEquipment;
    private Long availableEquipment;
    private Long reservedEquipment;
    private Long borrowedEquipment;
    private Long repairingEquipment;
    private Long brokenEquipment;
    
    private Long totalReservation;
    private Long pendingReservation;
    private Long approvedReservation;
    private Long rejectedReservation;
    
    private Long totalBorrow;
    private Long pendingBorrow;
    private Long approvedBorrow;
    private Long returnedBorrow;
    
    private Long totalRepair;
    private Long pendingRepair;
    private Long approvedRepair;
    private Long rejectedRepair;
    private Long completedRepair;
    
    private Long totalUser;
    private Long activeUser;
    
    private Long pendingReturn;
    private Long overdueBorrow;
    
    private Double equipmentUsageRate;
    private Double equipmentFaultRate;
    private Double repairCompletionRate;
    private Double averageRepairTime;
}
