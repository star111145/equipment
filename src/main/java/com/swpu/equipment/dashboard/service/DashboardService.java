package com.swpu.equipment.dashboard.service;

import com.swpu.equipment.dashboard.entity.*;

import java.util.List;

public interface DashboardService {
    
    DashboardStatistics getDashboardStatistics();
    
    List<EquipmentStatusCount> getEquipmentStatusCount();
    
    List<EquipmentUsageTrend> getEquipmentUsageTrend(String period);
    
    List<DashboardPendingAction> getDashboardPendingActions(Long userId, String role, String type);
    
    List<DashboardLatestRecord> getDashboardLatestRecords();
    
    List<ReservationHotspot> getReservationHotspots(int limit);
    
    RepairStatistics getRepairStatistics();
}
