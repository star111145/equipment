package com.swpu.equipment.dashboard.controller;

import com.swpu.equipment.common.util.Result;
import com.swpu.equipment.dashboard.entity.*;
import com.swpu.equipment.dashboard.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {
    
    @Autowired
    private DashboardService dashboardService;
    
    @GetMapping("/statistics")
    public Result<DashboardStatistics> getDashboardStatistics() {
        DashboardStatistics statistics = dashboardService.getDashboardStatistics();
        return Result.success(statistics);
    }
    
    @GetMapping("/equipment-status")
    public Result<List<EquipmentStatusCount>> getEquipmentStatusCount() {
        List<EquipmentStatusCount> counts = dashboardService.getEquipmentStatusCount();
        return Result.success(counts);
    }
    
    @GetMapping("/usage-trend")
    public Result<List<EquipmentUsageTrend>> getEquipmentUsageTrend(
            @RequestParam(defaultValue = "week") String period) {
        List<EquipmentUsageTrend> trends = dashboardService.getEquipmentUsageTrend(period);
        return Result.success(trends);
    }
    
    @GetMapping("/pending-actions")
    public Result<List<DashboardPendingAction>> getDashboardPendingActions(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String type) {
        List<DashboardPendingAction> actions = dashboardService.getDashboardPendingActions(userId, role, type);
        return Result.success(actions);
    }
    
    @GetMapping("/latest-records")
    public Result<List<DashboardLatestRecord>> getDashboardLatestRecords() {
        List<DashboardLatestRecord> records = dashboardService.getDashboardLatestRecords();
        return Result.success(records);
    }
    
    @GetMapping("/reservation-hotspots")
    public Result<List<ReservationHotspot>> getReservationHotspots(
            @RequestParam(defaultValue = "10") int limit) {
        List<ReservationHotspot> hotspots = dashboardService.getReservationHotspots(limit);
        return Result.success(hotspots);
    }
    
    @GetMapping("/repair-statistics")
    public Result<RepairStatistics> getRepairStatistics() {
        RepairStatistics statistics = dashboardService.getRepairStatistics();
        return Result.success(statistics);
    }
}
