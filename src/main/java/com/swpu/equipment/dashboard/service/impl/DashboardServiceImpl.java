package com.swpu.equipment.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.swpu.equipment.dashboard.entity.*;
import com.swpu.equipment.dashboard.service.DashboardService;
import com.swpu.equipment.equipment.entity.Equipment;
import com.swpu.equipment.equipment.mapper.EquipmentMapper;
import com.swpu.equipment.lifecycle.entity.EquipmentBorrow;
import com.swpu.equipment.lifecycle.entity.EquipmentReservation;
import com.swpu.equipment.lifecycle.entity.EquipmentRepair;
import com.swpu.equipment.lifecycle.entity.EquipmentReturn;
import com.swpu.equipment.lifecycle.mapper.EquipmentBorrowMapper;
import com.swpu.equipment.lifecycle.mapper.EquipmentReservationMapper;
import com.swpu.equipment.lifecycle.mapper.EquipmentRepairMapper;
import com.swpu.equipment.lifecycle.mapper.EquipmentReturnMapper;
import com.swpu.equipment.user.entity.User;
import com.swpu.equipment.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {
    
    @Autowired
    private EquipmentMapper equipmentMapper;
    
    @Autowired
    private EquipmentReservationMapper reservationMapper;
    
    @Autowired
    private EquipmentBorrowMapper borrowMapper;
    
    @Autowired
    private EquipmentRepairMapper repairMapper;
    
    @Autowired
    private EquipmentReturnMapper returnMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Override
    public DashboardStatistics getDashboardStatistics() {
        DashboardStatistics stats = new DashboardStatistics();
        
        List<Equipment> equipmentList = equipmentMapper.selectList(new LambdaQueryWrapper<>());
        stats.setTotalEquipment((long) equipmentList.size());
        
        Map<Integer, Long> statusCount = new HashMap<>();
        for (Equipment equipment : equipmentList) {
            Integer status = equipment.getEquipmentStatus();
            statusCount.put(status, statusCount.getOrDefault(status, 0L) + 1);
        }
        
        stats.setAvailableEquipment(statusCount.getOrDefault(1, 0L));
        stats.setReservedEquipment(statusCount.getOrDefault(2, 0L));
        stats.setBorrowedEquipment(statusCount.getOrDefault(3, 0L));
        stats.setRepairingEquipment(statusCount.getOrDefault(0, 0L));
        stats.setBrokenEquipment(statusCount.getOrDefault(4, 0L));
        
        List<EquipmentReservation> reservationList = reservationMapper.selectList(new LambdaQueryWrapper<>());
        stats.setTotalReservation((long) reservationList.size());
        
        Map<Integer, Long> reservationStatusCount = new HashMap<>();
        for (EquipmentReservation reservation : reservationList) {
            Integer status = reservation.getReserveStatus();
            reservationStatusCount.put(status, reservationStatusCount.getOrDefault(status, 0L) + 1);
        }
        
        stats.setPendingReservation(reservationStatusCount.getOrDefault(0, 0L));
        stats.setApprovedReservation(reservationStatusCount.getOrDefault(1, 0L));
        stats.setRejectedReservation(reservationStatusCount.getOrDefault(2, 0L));
        
        List<EquipmentBorrow> borrowList = borrowMapper.selectList(new LambdaQueryWrapper<>());
        stats.setTotalBorrow((long) borrowList.size());
        
        Map<Integer, Long> borrowStatusCount = new HashMap<>();
        for (EquipmentBorrow borrow : borrowList) {
            Integer status = borrow.getBorrowStatus();
            borrowStatusCount.put(status, borrowStatusCount.getOrDefault(status, 0L) + 1);
        }
        
        stats.setPendingBorrow(borrowStatusCount.getOrDefault(0, 0L));
        stats.setApprovedBorrow(borrowStatusCount.getOrDefault(1, 0L));
        stats.setReturnedBorrow(borrowStatusCount.getOrDefault(2, 0L));
        
        List<EquipmentRepair> repairList = repairMapper.selectList(new LambdaQueryWrapper<>());
        stats.setTotalRepair((long) repairList.size());
        
        Map<Integer, Long> repairStatusCount = new HashMap<>();
        for (EquipmentRepair repair : repairList) {
            Integer status = repair.getRepairStatus();
            repairStatusCount.put(status, repairStatusCount.getOrDefault(status, 0L) + 1);
        }
        
        stats.setPendingRepair(repairStatusCount.getOrDefault(0, 0L));
        stats.setApprovedRepair(repairStatusCount.getOrDefault(1, 0L));
        stats.setRejectedRepair(repairStatusCount.getOrDefault(2, 0L));
        
        List<User> userList = userMapper.selectList(new LambdaQueryWrapper<>());
        stats.setTotalUser((long) userList.size());
        
        return stats;
    }
    
    @Override
    public List<EquipmentStatusCount> getEquipmentStatusCount() {
        List<Equipment> equipmentList = equipmentMapper.selectList(new LambdaQueryWrapper<>());
        
        Map<Integer, Long> statusCount = new HashMap<>();
        for (Equipment equipment : equipmentList) {
            Integer status = equipment.getEquipmentStatus();
            statusCount.put(status, statusCount.getOrDefault(status, 0L) + 1);
        }
        
        List<EquipmentStatusCount> result = new ArrayList<>();
        String[] statusTexts = {"维修中", "空闲", "被预约", "已借用", "故障"};
        
        for (int i = 0; i < 5; i++) {
            EquipmentStatusCount count = new EquipmentStatusCount();
            count.setStatus(String.valueOf(i));
            count.setStatusText(statusTexts[i]);
            count.setCount(statusCount.getOrDefault(i, 0L));
            
            if (equipmentList.size() > 0) {
                count.setPercentage(Math.round((double) count.getCount() / equipmentList.size() * 10000) / 100.0);
            } else {
                count.setPercentage(0.0);
            }
            
            result.add(count);
        }
        
        return result;
    }
    
    @Override
    public List<EquipmentUsageTrend> getEquipmentUsageTrend(String period) {
        List<EquipmentUsageTrend> result = new ArrayList<>();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        int days = 7;
        
        if ("month".equals(period)) {
            days = 30;
        } else if ("semester".equals(period)) {
            days = 90;
        }
        
        for (int i = 0; i < days; i++) {
            EquipmentUsageTrend trend = new EquipmentUsageTrend();
            LocalDateTime date = LocalDateTime.now().minusDays(days - 1 - i);
            trend.setDate(date.format(formatter));
            trend.setBorrowCount(0L);
            trend.setReserveCount(0L);
            trend.setReturnCount(0L);
            trend.setRepairCount(0L);
            result.add(trend);
        }
        
        return result;
    }
    
    @Override
    public List<DashboardPendingAction> getDashboardPendingActions(Long userId, String role, String type) {
        List<DashboardPendingAction> pendingActions = new ArrayList<>();
        
        if ("admin".equals(role)) {
            if (type == null || "reservation".equals(type)) {
                LambdaQueryWrapper<EquipmentReservation> reservationQuery = new LambdaQueryWrapper<>();
                reservationQuery.eq(EquipmentReservation::getReserveStatus, 0)
                        .orderByDesc(EquipmentReservation::getCreateTime)
                        .last("LIMIT 5");
                List<EquipmentReservation> reservations = reservationMapper.selectList(reservationQuery);
                for (EquipmentReservation reservation : reservations) {
                    DashboardPendingAction action = new DashboardPendingAction();
                    action.setId(reservation.getId());
                    action.setTitle("预约申请: " + reservation.getEquipmentName());
                    action.setType("reservation");
                    action.setCreatedAt(reservation.getCreateTime());
                    action.setUpdatedAt(reservation.getUpdateTime());
                    action.setStatus("待审核");
                    action.setUserName(reservation.getRealName());
                    action.setEquipmentName(reservation.getEquipmentName());
                    pendingActions.add(action);
                }
            }
            
            if (type == null || "borrow".equals(type)) {
                LambdaQueryWrapper<EquipmentBorrow> borrowQuery = new LambdaQueryWrapper<>();
                borrowQuery.eq(EquipmentBorrow::getBorrowStatus, 0)
                        .orderByDesc(EquipmentBorrow::getCreateTime)
                        .last("LIMIT 5");
                List<EquipmentBorrow> borrows = borrowMapper.selectList(borrowQuery);
                for (EquipmentBorrow borrow : borrows) {
                    DashboardPendingAction action = new DashboardPendingAction();
                    action.setId(borrow.getId());
                    action.setTitle("借用申请: " + borrow.getEquipmentName());
                    action.setType("borrow");
                    action.setCreatedAt(borrow.getCreateTime());
                    action.setUpdatedAt(borrow.getUpdateTime());
                    action.setStatus("待审核");
                    action.setUserName(borrow.getRealName());
                    action.setEquipmentName(borrow.getEquipmentName());
                    pendingActions.add(action);
                }
                
                if (userId != null) {
                    LambdaQueryWrapper<EquipmentBorrow> userBorrowQuery = new LambdaQueryWrapper<>();
                    userBorrowQuery.eq(EquipmentBorrow::getUserId, userId)
                            .eq(EquipmentBorrow::getBorrowStatus, 1)
                            .orderByDesc(EquipmentBorrow::getCreateTime)
                            .last("LIMIT 5");
                    List<EquipmentBorrow> userBorrows = borrowMapper.selectList(userBorrowQuery);
                    for (EquipmentBorrow borrow : userBorrows) {
                        DashboardPendingAction action = new DashboardPendingAction();
                        action.setId(borrow.getId());
                        action.setTitle("您有借用设备: " + borrow.getEquipmentName() + "，请尽早归还");
                        action.setType("borrow");
                        action.setCreatedAt(borrow.getCreateTime());
                        action.setUpdatedAt(borrow.getUpdateTime());
                        action.setStatus("使用中");
                        action.setUserName(borrow.getRealName());
                        action.setEquipmentName(borrow.getEquipmentName());
                        pendingActions.add(action);
                    }
                }
            }
            
            if (type == null || "repair".equals(type)) {
                LambdaQueryWrapper<EquipmentRepair> repairQuery = new LambdaQueryWrapper<>();
                repairQuery.in(EquipmentRepair::getRepairStatus, Arrays.asList(0, 1))
                        .orderByDesc(EquipmentRepair::getCreateTime)
                        .last("LIMIT 5");
                List<EquipmentRepair> repairs = repairMapper.selectList(repairQuery);
                for (EquipmentRepair repair : repairs) {
                    DashboardPendingAction action = new DashboardPendingAction();
                    action.setId(repair.getId());
                    String statusText = repair.getRepairStatus() == 0 ? "待审核" : "维修中";
                    action.setTitle("报修申请: " + repair.getEquipmentName());
                    action.setType("repair");
                    action.setCreatedAt(repair.getCreateTime());
                    action.setUpdatedAt(repair.getUpdateTime());
                    action.setStatus(statusText);
                    action.setUserName(repair.getRealName());
                    action.setEquipmentName(repair.getEquipmentName());
                    pendingActions.add(action);
                }
            }
            
            if (type == null || "return".equals(type)) {
                LambdaQueryWrapper<EquipmentReturn> returnQuery = new LambdaQueryWrapper<>();
                returnQuery.eq(EquipmentReturn::getReturnStatus, 0)
                        .orderByDesc(EquipmentReturn::getCreateTime)
                        .last("LIMIT 5");
                List<EquipmentReturn> returns = returnMapper.selectList(returnQuery);
                for (EquipmentReturn ret : returns) {
                    DashboardPendingAction action = new DashboardPendingAction();
                    action.setId(ret.getId());
                    action.setTitle("归还申请: " + ret.getEquipmentName());
                    action.setType("return");
                    action.setCreatedAt(ret.getCreateTime());
                    action.setUpdatedAt(ret.getUpdateTime());
                    action.setStatus("待审核");
                    action.setUserName(ret.getRealName());
                    action.setEquipmentName(ret.getEquipmentName());
                    pendingActions.add(action);
                }
            }
        } else if (userId != null) {
            LambdaQueryWrapper<EquipmentBorrow> borrowQuery = new LambdaQueryWrapper<>();
            borrowQuery.eq(EquipmentBorrow::getUserId, userId)
                    .eq(EquipmentBorrow::getBorrowStatus, 1)
                    .orderByDesc(EquipmentBorrow::getCreateTime)
                    .last("LIMIT 5");
            List<EquipmentBorrow> borrows = borrowMapper.selectList(borrowQuery);
            for (EquipmentBorrow borrow : borrows) {
                DashboardPendingAction action = new DashboardPendingAction();
                action.setId(borrow.getId());
                action.setTitle("您有借用设备: " + borrow.getEquipmentName() + "，请尽早归还");
                action.setType("borrow");
                action.setCreatedAt(borrow.getCreateTime());
                action.setUpdatedAt(borrow.getUpdateTime());
                action.setStatus("使用中");
                action.setUserName(borrow.getRealName());
                action.setEquipmentName(borrow.getEquipmentName());
                pendingActions.add(action);
            }
            
            LambdaQueryWrapper<EquipmentReservation> reserveQuery = new LambdaQueryWrapper<>();
            reserveQuery.eq(EquipmentReservation::getUserId, userId)
                    .eq(EquipmentReservation::getReserveStatus, 1)
                    .eq(EquipmentReservation::getAuditStatus, 1)
                    .orderByDesc(EquipmentReservation::getCreateTime)
                    .last("LIMIT 5");
            List<EquipmentReservation> reservations = reservationMapper.selectList(reserveQuery);
            LocalDateTime now = LocalDateTime.now();
            for (EquipmentReservation reservation : reservations) {
                LocalDateTime startTime = reservation.getReserveTime();
                LocalDateTime endTime = reservation.getReserveTime().plusHours(reservation.getReserveDuration());
                if (!startTime.isAfter(now) && endTime.isAfter(now)) {
                    DashboardPendingAction action = new DashboardPendingAction();
                    action.setId(reservation.getId());
                    action.setTitle("预约成功: " + reservation.getEquipmentName() + "，可以来借用设备了");
                    action.setType("reservation");
                    action.setCreatedAt(reservation.getCreateTime());
                    action.setUpdatedAt(reservation.getReserveTime());
                    action.setStatus("可借用");
                    action.setUserName(reservation.getRealName());
                    action.setEquipmentName(reservation.getEquipmentName());
                    pendingActions.add(action);
                }
            }
        }
        
        pendingActions.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        
        return pendingActions.subList(0, Math.min(pendingActions.size(), 10));
    }
    
    @Override
    public List<DashboardLatestRecord> getDashboardLatestRecords() {
        List<DashboardLatestRecord> latestRecords = new ArrayList<>();
        
        LambdaQueryWrapper<EquipmentReservation> reservationQuery = new LambdaQueryWrapper<>();
        reservationQuery.eq(EquipmentReservation::getReserveStatus, 1)
                .orderByDesc(EquipmentReservation::getCreateTime)
                .last("LIMIT 5");
        List<EquipmentReservation> reservations = reservationMapper.selectList(reservationQuery);
        for (EquipmentReservation reservation : reservations) {
            DashboardLatestRecord record = new DashboardLatestRecord();
            record.setId(reservation.getId());
            record.setTitle("预约: " + reservation.getEquipmentName());
            record.setType("reservation");
            record.setCreatedAt(reservation.getCreateTime());
            record.setStatus(getReservationStatusText(reservation.getReserveStatus()));
            record.setUserName(reservation.getRealName());
            record.setUserId(reservation.getUserId());
            record.setEquipmentName(reservation.getEquipmentName());
            latestRecords.add(record);
        }
        
        LambdaQueryWrapper<EquipmentBorrow> borrowQuery = new LambdaQueryWrapper<>();
        borrowQuery.eq(EquipmentBorrow::getBorrowStatus, 1)
                .orderByDesc(EquipmentBorrow::getCreateTime)
                .last("LIMIT 5");
        List<EquipmentBorrow> borrows = borrowMapper.selectList(borrowQuery);
        for (EquipmentBorrow borrow : borrows) {
            DashboardLatestRecord record = new DashboardLatestRecord();
            record.setId(borrow.getId());
            record.setTitle("借用: " + borrow.getEquipmentName());
            record.setType("borrow");
            record.setCreatedAt(borrow.getCreateTime());
            record.setStatus(getBorrowStatusText(borrow.getBorrowStatus()));
            record.setUserName(borrow.getRealName());
            record.setUserId(borrow.getUserId());
            record.setEquipmentName(borrow.getEquipmentName());
            latestRecords.add(record);
        }
        
        LambdaQueryWrapper<EquipmentRepair> repairQuery = new LambdaQueryWrapper<>();
        repairQuery.eq(EquipmentRepair::getRepairStatus, 1)
                .orderByDesc(EquipmentRepair::getCreateTime)
                .last("LIMIT 5");
        List<EquipmentRepair> repairs = repairMapper.selectList(repairQuery);
        for (EquipmentRepair repair : repairs) {
            DashboardLatestRecord record = new DashboardLatestRecord();
            record.setId(repair.getId());
            record.setTitle("报修: " + repair.getEquipmentName());
            record.setType("repair");
            record.setCreatedAt(repair.getCreateTime());
            record.setStatus(getRepairStatusText(repair.getRepairStatus()));
            record.setUserName(repair.getRealName());
            record.setUserId(repair.getUserId());
            record.setEquipmentName(repair.getEquipmentName());
            latestRecords.add(record);
        }
        
        LambdaQueryWrapper<EquipmentReturn> returnQuery = new LambdaQueryWrapper<>();
        returnQuery.eq(EquipmentReturn::getReturnStatus, 1)
                .orderByDesc(EquipmentReturn::getCreateTime)
                .last("LIMIT 5");
        List<EquipmentReturn> returns = returnMapper.selectList(returnQuery);
        for (EquipmentReturn returnRecord : returns) {
            DashboardLatestRecord record = new DashboardLatestRecord();
            record.setId(returnRecord.getId());
            record.setTitle("归还: " + returnRecord.getEquipmentName());
            record.setType("return");
            record.setCreatedAt(returnRecord.getCreateTime());
            record.setStatus(getReturnStatusText(returnRecord.getReturnStatus()));
            record.setUserName(returnRecord.getRealName());
            record.setUserId(returnRecord.getUserId());
            record.setEquipmentName(returnRecord.getEquipmentName());
            latestRecords.add(record);
        }
        
        latestRecords.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        
        return latestRecords.subList(0, Math.min(latestRecords.size(), 15));
    }
    
    private String getReservationStatusText(Integer status) {
        switch (status) {
            case 0: return "待审核";
            case 1: return "已通过";
            case 2: return "已拒绝";
            case 3: return "已取消";
            default: return "未知";
        }
    }
    
    private String getBorrowStatusText(Integer status) {
        switch (status) {
            case 0: return "待审核";
            case 1: return "已借出";
            case 2: return "已完成";
            case 3: return "已取消";
            default: return "未知";
        }
    }
    
    private String getRepairStatusText(Integer status) {
        switch (status) {
            case 0: return "待审核";
            case 1: return "报修中";
            case 2: return "已维修";
            case 3: return "已拒绝";
            case 4: return "已取消";
            default: return "未知";
        }
    }
    
    private String getReturnStatusText(Integer status) {
        switch (status) {
            case 0: return "待归还";
            case 1: return "已归还";
            case 2: return "已拒绝";
            default: return "未知";
        }
    }
    
    @Override
    public List<ReservationHotspot> getReservationHotspots(int limit) {
        List<ReservationHotspot> hotspots = new ArrayList<>();
        
        LambdaQueryWrapper<EquipmentReservation> query = new LambdaQueryWrapper<>();
        query.eq(EquipmentReservation::getReserveStatus, 1);
        
        List<EquipmentReservation> allReservations = reservationMapper.selectList(query);
        
        Map<Long, Integer> countMap = new HashMap<>();
        Map<Long, String> nameMap = new HashMap<>();
        
        for (EquipmentReservation r : allReservations) {
            if (r.getEquipmentId() != null) {
                countMap.put(r.getEquipmentId(), countMap.getOrDefault(r.getEquipmentId(), 0) + 1);
                if (r.getEquipmentName() != null) {
                    nameMap.put(r.getEquipmentId(), r.getEquipmentName());
                }
            }
        }
        
        for (Map.Entry<Long, Integer> entry : countMap.entrySet()) {
            ReservationHotspot hotspot = new ReservationHotspot();
            hotspot.setEquipmentName(nameMap.getOrDefault(entry.getKey(), "未知设备"));
            hotspot.setReservationCount(entry.getValue());
            hotspots.add(hotspot);
        }
        
        hotspots.sort((a, b) -> b.getReservationCount().compareTo(a.getReservationCount()));
        
        return hotspots.subList(0, Math.min(hotspots.size(), limit));
    }
    
    @Override
    public RepairStatistics getRepairStatistics() {
        RepairStatistics stats = new RepairStatistics();
        
        List<EquipmentRepair> repairList = repairMapper.selectList(new LambdaQueryWrapper<>());
        stats.setTotalRepairs(repairList.size());
        
        Map<Integer, Long> statusCount = new HashMap<>();
        for (EquipmentRepair repair : repairList) {
            Integer status = repair.getRepairStatus();
            statusCount.put(status, statusCount.getOrDefault(status, 0L) + 1);
        }
        
        stats.setPendingRepairs(statusCount.getOrDefault(0, 0L).intValue());
        stats.setInProgressRepairs(statusCount.getOrDefault(1, 0L).intValue());
        stats.setCompletedRepairs(statusCount.getOrDefault(2, 0L).intValue());
        stats.setRejectedRepairs(statusCount.getOrDefault(3, 0L).intValue());
        
        double totalTime = 0;
        int count = 0;
        for (EquipmentRepair repair : repairList) {
            if (repair.getRepairStatus() == 2 && repair.getCreateTime() != null && repair.getUpdateTime() != null) {
                long hours = java.time.Duration.between(repair.getCreateTime(), repair.getUpdateTime()).toHours();
                totalTime += hours;
                count++;
            }
        }
        stats.setAverageProcessingTime(count > 0 ? Math.round(totalTime / count * 10) / 10.0 : 0.0);
        
        return stats;
    }
}
