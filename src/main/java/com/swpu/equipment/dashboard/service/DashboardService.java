package com.swpu.equipment.dashboard.service;

import com.swpu.equipment.dashboard.entity.*;

import java.util.List;

public interface DashboardService {
    
    DashboardStatistics getDashboardStatistics(String equipmentType, Long equipmentId, Long userId, String role);
    
    List<EquipmentStatusCount> getEquipmentStatusCount(String equipmentType, Long equipmentId, Long userId, String role);
    
    List<EquipmentUsageTrend> getEquipmentUsageTrend(String period, String equipmentType, Long equipmentId, Long userId, String role);
    
    List<DashboardPendingAction> getDashboardPendingActions(Long userId, String role, String type);
    
    List<DashboardLatestRecord> getDashboardLatestRecords();
    
    List<ReservationHotspot> getReservationHotspots(Integer limit, String equipmentType, Long equipmentId, Long userId, String role);
    
    RepairStatistics getRepairStatistics(String equipmentType, Long equipmentId, Long userId, String role);
}
