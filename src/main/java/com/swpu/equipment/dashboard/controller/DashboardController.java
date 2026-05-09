package com.swpu.equipment.dashboard.controller;

import com.alibaba.excel.EasyExcel;
import com.swpu.equipment.common.util.Result;
import com.swpu.equipment.dashboard.entity.*;
import com.swpu.equipment.dashboard.service.DashboardService;
import com.swpu.equipment.dashboard.export.DashboardExcelData;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Dashboard controller providing statistical and status information
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {
    
    @Autowired
    private DashboardService dashboardService;
    
    @GetMapping("/statistics")
    public Result<DashboardStatistics> getDashboardStatistics(
            @RequestParam(required = false) String equipmentType,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String role) {
        DashboardStatistics statistics = dashboardService.getDashboardStatistics(equipmentType, equipmentId, userId, role);
        return Result.success(statistics);
    }
    
    @GetMapping("/equipment-status")
    public Result<List<EquipmentStatusCount>> getEquipmentStatusCount(
            @RequestParam(required = false) String equipmentType,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String role) {
        List<EquipmentStatusCount> counts = dashboardService.getEquipmentStatusCount(equipmentType, equipmentId, userId, role);
        return Result.success(counts);
    }
    
    @GetMapping("/usage-trend")
    public Result<List<EquipmentUsageTrend>> getEquipmentUsageTrend(
            @RequestParam(defaultValue = "week") String period,
            @RequestParam(required = false) String equipmentType,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String role) {
        List<EquipmentUsageTrend> trends = dashboardService.getEquipmentUsageTrend(period, equipmentType, equipmentId, userId, role);
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
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) String equipmentType,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String role) {
        List<ReservationHotspot> hotspots = dashboardService.getReservationHotspots(limit, equipmentType, equipmentId, userId, role);
        return Result.success(hotspots);
    }
    
    @GetMapping("/repair-statistics")
    public Result<RepairStatistics> getRepairStatistics(
            @RequestParam(required = false) String equipmentType,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String role) {
        RepairStatistics statistics = dashboardService.getRepairStatistics(equipmentType, equipmentId, userId, role);
        return Result.success(statistics);
    }
    
    @GetMapping("/export")
    public void exportDashboard(
            @RequestParam(defaultValue = "all") String reportType,
            @RequestParam(required = false) String equipmentType,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "week") String period,
            HttpServletResponse response) throws IOException {
        
        List<DashboardExcelData> dataList = dashboardService.getExportData(reportType, equipmentType, equipmentId, userId, role, period);
        
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = "数据统计报表_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        
        EasyExcel.write(response.getOutputStream(), DashboardExcelData.class)
                .sheet("数据统计")
                .doWrite(dataList);
    }
}
