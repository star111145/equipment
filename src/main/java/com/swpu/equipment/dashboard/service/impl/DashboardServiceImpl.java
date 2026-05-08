package com.swpu.equipment.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.swpu.equipment.dashboard.entity.*;
import com.swpu.equipment.dashboard.service.DashboardService;
import com.swpu.equipment.equipment.entity.Equipment;
import com.swpu.equipment.equipment.entity.EquipmentType;
import com.swpu.equipment.equipment.mapper.EquipmentMapper;
import com.swpu.equipment.equipment.mapper.EquipmentTypeMapper;
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

import java.util.*;

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
    private EquipmentTypeMapper equipmentTypeMapper;
    
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
    
    private Set<Long> getFilteredEquipmentIds(String equipmentType, Long equipmentId) {
        Set<Long> filteredIds = new HashSet<>();
        
        Map<String, Long> nameToTypeIdMap = new HashMap<>();
        List<EquipmentType> allTypes = equipmentTypeMapper.selectList(new LambdaQueryWrapper<>());
        for (EquipmentType t : allTypes) {
            nameToTypeIdMap.put(t.getTypeName(), t.getId());
        }
        
        LambdaQueryWrapper<Equipment> query = new LambdaQueryWrapper<>();
        
        if (equipmentId != null) {
            query.eq(Equipment::getId, equipmentId);
        } else if (equipmentType != null && !equipmentType.isEmpty()) {
            Long typeId = nameToTypeIdMap.get(equipmentType);
            if (typeId != null) {
                query.eq(Equipment::getEquipmentTypeId, typeId);
            }
        }
        
        List<Equipment> equipmentList = equipmentMapper.selectList(query);
        for (Equipment e : equipmentList) {
            filteredIds.add(e.getId());
        }
        
        return filteredIds;
    }
    
    /**
     * 获取用户使用过的设备ID集合（预约过、借用过、报修过的设备）
     */
    private Set<Long> getUserEquipmentIds(Long userId) {
        Set<Long> userEquipmentIds = new HashSet<>();
        
        // 查询用户预约过的设备
        LambdaQueryWrapper<EquipmentReservation> resQuery = new LambdaQueryWrapper<>();
        resQuery.eq(EquipmentReservation::getUserId, userId);
        List<EquipmentReservation> reservations = reservationMapper.selectList(resQuery);
        for (EquipmentReservation r : reservations) {
            if (r.getEquipmentId() != null) {
                userEquipmentIds.add(r.getEquipmentId());
            }
        }
        
        // 查询用户借用过的设备
        LambdaQueryWrapper<EquipmentBorrow> borrowQuery = new LambdaQueryWrapper<>();
        borrowQuery.eq(EquipmentBorrow::getUserId, userId);
        List<EquipmentBorrow> borrows = borrowMapper.selectList(borrowQuery);
        for (EquipmentBorrow b : borrows) {
            if (b.getEquipmentId() != null) {
                userEquipmentIds.add(b.getEquipmentId());
            }
        }
        
        // 查询用户报修过的设备
        LambdaQueryWrapper<EquipmentRepair> repairQuery = new LambdaQueryWrapper<>();
        repairQuery.eq(EquipmentRepair::getUserId, userId);
        List<EquipmentRepair> repairs = repairMapper.selectList(repairQuery);
        for (EquipmentRepair r : repairs) {
            if (r.getEquipmentId() != null) {
                userEquipmentIds.add(r.getEquipmentId());
            }
        }
        
        return userEquipmentIds;
    }
    
    /**
     * 计算设备当前状态（基于实际预约/借用记录，动态计算）
     * 优化：避免N+1查询问题，先批量查询所有预约和借用记录，然后在内存中处理
     */
    private Map<String, Long> calculateCurrentStatusCount(Set<Long> equipmentIds, Long userId, boolean isAdmin) {
        Map<String, Long> statusCount = new HashMap<>();
        
        LambdaQueryWrapper<Equipment> query = new LambdaQueryWrapper<>();
        if (!equipmentIds.isEmpty()) {
            query.in(Equipment::getId, equipmentIds);
        }
        List<Equipment> allEquipment = equipmentMapper.selectList(query);
        
        if (allEquipment.isEmpty()) {
            return statusCount;
        }
        
        Set<Long> allEquipmentIds = new HashSet<>();
        for (Equipment e : allEquipment) {
            allEquipmentIds.add(e.getId());
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // 普通用户：只统计用户借用/预约的设备状态
        Set<Long> userEquipmentIds = new HashSet<>();
        if (!isAdmin && userId != null) {
            userEquipmentIds = getUserEquipmentIds(userId);
            if (!equipmentIds.isEmpty()) {
                userEquipmentIds.retainAll(equipmentIds);
            }
        }
        
        // 批量查询所有有效预约（状态=1已通过），然后在内存中过滤未过期的记录
        LambdaQueryWrapper<EquipmentReservation> resQuery = new LambdaQueryWrapper<>();
        resQuery.in(EquipmentReservation::getEquipmentId, allEquipmentIds)
                .eq(EquipmentReservation::getReserveStatus, 1);
        List<EquipmentReservation> allReservations = reservationMapper.selectList(resQuery);
        
        Set<Long> reservedEquipmentIds = new HashSet<>();
        for (EquipmentReservation r : allReservations) {
            if (r.getReserveTime() != null && r.getEquipmentId() != null) {
                LocalDateTime reserveEnd = r.getReserveTime().plusHours(r.getReserveDuration() != null ? r.getReserveDuration() : 0);
                if (reserveEnd.isAfter(now)) {
                    reservedEquipmentIds.add(r.getEquipmentId());
                }
            }
        }
        
        LambdaQueryWrapper<EquipmentBorrow> borrowQuery = new LambdaQueryWrapper<>();
        borrowQuery.in(EquipmentBorrow::getEquipmentId, allEquipmentIds)
                  .eq(EquipmentBorrow::getBorrowStatus, 1);
        List<EquipmentBorrow> allBorrows = borrowMapper.selectList(borrowQuery);
        
        Set<Long> borrowedEquipmentIds = new HashSet<>();
        for (EquipmentBorrow b : allBorrows) {
            if (b.getEquipmentId() != null) {
                borrowedEquipmentIds.add(b.getEquipmentId());
            }
        }
        
        // 查询维修中的设备（报修状态=1）
        LambdaQueryWrapper<EquipmentRepair> repairQuery = new LambdaQueryWrapper<>();
        repairQuery.in(EquipmentRepair::getEquipmentId, allEquipmentIds)
                  .eq(EquipmentRepair::getRepairStatus, 1);
        List<EquipmentRepair> allRepairs = repairMapper.selectList(repairQuery);
        
        Set<Long> repairingEquipmentIds = new HashSet<>();
        for (EquipmentRepair r : allRepairs) {
            if (r.getEquipmentId() != null) {
                repairingEquipmentIds.add(r.getEquipmentId());
            }
        }
        
        // 统计设备状态：只按实际预约/借用/维修记录计算
        // 设备表的静态状态仅作为管理员提醒，不参与统计计算
        for (Equipment e : allEquipment) {
            int stock = e.getStockQuantity() != null ? e.getStockQuantity() : 1;
            
            // 按实际预约/借用/维修状态计算
            if (repairingEquipmentIds.contains(e.getId())) {
                statusCount.put("repairing", statusCount.getOrDefault("repairing", 0L) + stock);
            } else if (reservedEquipmentIds.contains(e.getId())) {
                statusCount.put("reserved", statusCount.getOrDefault("reserved", 0L) + stock);
            } else if (borrowedEquipmentIds.contains(e.getId())) {
                statusCount.put("borrowed", statusCount.getOrDefault("borrowed", 0L) + stock);
            } else {
                statusCount.put("available", statusCount.getOrDefault("available", 0L) + stock);
            }
        }
        
        return statusCount;
    }
    
    @Override
    public DashboardStatistics getDashboardStatistics(String equipmentType, Long equipmentId, Long userId, String role) {
        DashboardStatistics stats = new DashboardStatistics();
        
        boolean isAdmin = "admin".equals(role);
        
        Set<Long> filteredEquipmentIds = getFilteredEquipmentIds(equipmentType, equipmentId);
        
        Set<Long> userEquipmentIds = new HashSet<>();
        if (!isAdmin && userId != null) {
            userEquipmentIds = getUserEquipmentIds(userId);
            if (equipmentId == null && equipmentType == null) {
                filteredEquipmentIds = userEquipmentIds;
            }
        }
        
        LambdaQueryWrapper<Equipment> eq = new LambdaQueryWrapper<>();
        if (equipmentId != null) {
            eq.eq(Equipment::getId, equipmentId);
        } else if (equipmentType != null && !equipmentType.isEmpty()) {
            Map<String, Long> nameToTypeIdMap = new HashMap<>();
            List<EquipmentType> allTypes = equipmentTypeMapper.selectList(new LambdaQueryWrapper<>());
            for (EquipmentType t : allTypes) {
                nameToTypeIdMap.put(t.getTypeName(), t.getId());
            }
            Long typeId = nameToTypeIdMap.get(equipmentType);
            if (typeId != null) {
                eq.eq(Equipment::getEquipmentTypeId, typeId);
            }
        }
        if (!isAdmin && userId != null && equipmentId == null && equipmentType == null) {
            eq.in(Equipment::getId, userEquipmentIds);
        }
        List<Equipment> equipmentList = equipmentMapper.selectList(eq);
        
        long totalStock = 0;
        for (Equipment e : equipmentList) {
            totalStock += e.getStockQuantity() != null ? e.getStockQuantity() : 1;
        }
        stats.setTotalEquipment(totalStock);
        
        Map<String, Long> currentStatusCount = calculateCurrentStatusCount(filteredEquipmentIds, userId, isAdmin);
        
        stats.setAvailableEquipment(currentStatusCount.getOrDefault("available", 0L));
        stats.setReservedEquipment(currentStatusCount.getOrDefault("reserved", 0L));
        stats.setBorrowedEquipment(currentStatusCount.getOrDefault("borrowed", 0L));
        stats.setRepairingEquipment(currentStatusCount.getOrDefault("repairing", 0L));
        stats.setBrokenEquipment(currentStatusCount.getOrDefault("broken", 0L));
        
        LambdaQueryWrapper<EquipmentReservation> rq = new LambdaQueryWrapper<>();
        if (equipmentId != null) {
            rq.eq(EquipmentReservation::getEquipmentId, equipmentId);
        } else if (!filteredEquipmentIds.isEmpty()) {
            rq.in(EquipmentReservation::getEquipmentId, filteredEquipmentIds);
        }
        if (!isAdmin && userId != null) {
            rq.eq(EquipmentReservation::getUserId, userId);
        }
        List<EquipmentReservation> reservationList = reservationMapper.selectList(rq);
        
        stats.setTotalReservation((long) reservationList.size());
        
        Map<Integer, Long> reservationStatusCount = new HashMap<>();
        for (EquipmentReservation reservation : reservationList) {
            Integer status = reservation.getReserveStatus();
            reservationStatusCount.put(status, reservationStatusCount.getOrDefault(status, 0L) + 1);
        }
        
        stats.setPendingReservation(reservationStatusCount.getOrDefault(0, 0L));
        stats.setApprovedReservation(reservationStatusCount.getOrDefault(1, 0L));
        stats.setRejectedReservation(reservationStatusCount.getOrDefault(2, 0L));
        
        LambdaQueryWrapper<EquipmentBorrow> bk = new LambdaQueryWrapper<>();
        if (equipmentId != null) {
            bk.eq(EquipmentBorrow::getEquipmentId, equipmentId);
        } else if (!filteredEquipmentIds.isEmpty()) {
            bk.in(EquipmentBorrow::getEquipmentId, filteredEquipmentIds);
        }
        if (!isAdmin && userId != null) {
            bk.eq(EquipmentBorrow::getUserId, userId);
        }
        List<EquipmentBorrow> borrowList = borrowMapper.selectList(bk);
        stats.setTotalBorrow((long) borrowList.size());
        
        Map<Integer, Long> borrowStatusCount = new HashMap<>();
        for (EquipmentBorrow borrow : borrowList) {
            Integer status = borrow.getBorrowStatus();
            borrowStatusCount.put(status, borrowStatusCount.getOrDefault(status, 0L) + 1);
        }
        
        stats.setPendingBorrow(borrowStatusCount.getOrDefault(0, 0L));
        stats.setApprovedBorrow(borrowStatusCount.getOrDefault(1, 0L));
        stats.setReturnedBorrow(borrowStatusCount.getOrDefault(2, 0L));
        
        LambdaQueryWrapper<EquipmentRepair> rp = new LambdaQueryWrapper<>();
        if (equipmentId != null) {
            rp.eq(EquipmentRepair::getEquipmentId, equipmentId);
        } else if (!filteredEquipmentIds.isEmpty()) {
            rp.in(EquipmentRepair::getEquipmentId, filteredEquipmentIds);
        }
        if (!isAdmin && userId != null) {
            rp.eq(EquipmentRepair::getUserId, userId);
        }
        List<EquipmentRepair> repairList = repairMapper.selectList(rp);
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
    public List<EquipmentStatusCount> getEquipmentStatusCount(String equipmentType, Long equipmentId, Long userId, String role) {
        boolean isAdmin = "admin".equals(role);
        
        Set<Long> filteredEquipmentIds = getFilteredEquipmentIds(equipmentType, equipmentId);
        
        if (!isAdmin && userId != null) {
            Set<Long> userEquipmentIds = getUserEquipmentIds(userId);
            if (equipmentId == null && equipmentType == null) {
                filteredEquipmentIds = userEquipmentIds;
            }
        }
        
        Map<String, Long> currentStatusCount = calculateCurrentStatusCount(filteredEquipmentIds, userId, isAdmin);
        
        long totalCount = currentStatusCount.values().stream().mapToLong(Long::longValue).sum();
        
        List<EquipmentStatusCount> result = new ArrayList<>();
        String[] statusKeys = {"repairing", "available", "reserved", "borrowed", "broken"};
        String[] statusTexts = {"维修中", "空闲", "被预约", "已借用", "故障"};
        
        for (int i = 0; i < 5; i++) {
            EquipmentStatusCount count = new EquipmentStatusCount();
            count.setStatus(String.valueOf(i));
            count.setStatusText(statusTexts[i]);
            count.setCount(currentStatusCount.getOrDefault(statusKeys[i], 0L));
            
            if (totalCount > 0) {
                count.setPercentage(Math.round((double) count.getCount() / totalCount * 10000) / 100.0);
            } else {
                count.setPercentage(0.0);
            }
            
            result.add(count);
        }
        
        return result;
    }
    
    @Override
    public List<EquipmentUsageTrend> getEquipmentUsageTrend(String period, String equipmentType, Long equipmentId, Long userId, String role) {
        boolean isAdmin = "admin".equals(role);
        
        List<EquipmentUsageTrend> result = new ArrayList<>();
        
        Set<Long> filteredEquipmentIds = getFilteredEquipmentIds(equipmentType, equipmentId);
        
        if (!isAdmin && userId != null) {
            Set<Long> userEquipmentIds = getUserEquipmentIds(userId);
            if (equipmentId == null && equipmentType == null) {
                filteredEquipmentIds = userEquipmentIds;
            }
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        int days = 7;
        
        if ("month".equals(period)) {
            days = 30;
        } else if ("semester".equals(period)) {
            days = 90;
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < days; i++) {
            LocalDateTime targetDate = now.minusDays(days - 1 - i);
            String dateStr = targetDate.format(formatter);
            
            EquipmentUsageTrend trend = new EquipmentUsageTrend();
            trend.setDate(dateStr);
            
            if (!filteredEquipmentIds.isEmpty()) {
                LocalDateTime dayStart = targetDate.toLocalDate().atStartOfDay();
                LocalDateTime dayEnd = dayStart.plusDays(1);
                
                // 统计预约次数
                LambdaQueryWrapper<EquipmentReservation> resQuery = new LambdaQueryWrapper<>();
                resQuery.in(EquipmentReservation::getEquipmentId, filteredEquipmentIds)
                        .ge(EquipmentReservation::getCreateTime, dayStart)
                        .lt(EquipmentReservation::getCreateTime, dayEnd);
                if (!isAdmin && userId != null) {
                    resQuery.eq(EquipmentReservation::getUserId, userId);
                }
                Long reserveCount = reservationMapper.selectCount(resQuery);
                trend.setReserveCount(reserveCount);
                
                // 统计借用次数
                LambdaQueryWrapper<EquipmentBorrow> borrowQuery = new LambdaQueryWrapper<>();
                borrowQuery.in(EquipmentBorrow::getEquipmentId, filteredEquipmentIds)
                         .ge(EquipmentBorrow::getCreateTime, dayStart)
                         .lt(EquipmentBorrow::getCreateTime, dayEnd);
                if (!isAdmin && userId != null) {
                    borrowQuery.eq(EquipmentBorrow::getUserId, userId);
                }
                Long borrowCount = borrowMapper.selectCount(borrowQuery);
                trend.setBorrowCount(borrowCount);
                
                // 统计归还次数
                LambdaQueryWrapper<EquipmentReturn> returnQuery = new LambdaQueryWrapper<>();
                returnQuery.in(EquipmentReturn::getEquipmentId, filteredEquipmentIds)
                          .ge(EquipmentReturn::getCreateTime, dayStart)
                          .lt(EquipmentReturn::getCreateTime, dayEnd);
                if (!isAdmin && userId != null) {
                    returnQuery.eq(EquipmentReturn::getUserId, userId);
                }
                Long returnCount = returnMapper.selectCount(returnQuery);
                trend.setReturnCount(returnCount);
                
                // 统计报修次数
                LambdaQueryWrapper<EquipmentRepair> repairQuery = new LambdaQueryWrapper<>();
                repairQuery.in(EquipmentRepair::getEquipmentId, filteredEquipmentIds)
                          .ge(EquipmentRepair::getCreateTime, dayStart)
                          .lt(EquipmentRepair::getCreateTime, dayEnd);
                if (!isAdmin && userId != null) {
                    repairQuery.eq(EquipmentRepair::getUserId, userId);
                }
                Long repairCount = repairMapper.selectCount(repairQuery);
                trend.setRepairCount(repairCount);
            } else {
                trend.setBorrowCount(0L);
                trend.setReserveCount(0L);
                trend.setReturnCount(0L);
                trend.setRepairCount(0L);
            }
            
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
    public List<ReservationHotspot> getReservationHotspots(Integer limit, String equipmentType, Long equipmentId, Long userId, String role) {
        boolean isAdmin = "admin".equals(role);
        
        List<ReservationHotspot> hotspots = new ArrayList<>();
        
        LambdaQueryWrapper<EquipmentReservation> query = new LambdaQueryWrapper<>();
        query.eq(EquipmentReservation::getReserveStatus, 1);
        
        if (equipmentId != null) {
            query.eq(EquipmentReservation::getEquipmentId, equipmentId);
        }
        
        if (!isAdmin && userId != null) {
            query.eq(EquipmentReservation::getUserId, userId);
        }
        
        List<EquipmentReservation> allReservations = reservationMapper.selectList(query);
        
        Map<Long, Integer> countMap = new HashMap<>();
        Map<Long, String> nameMap = new HashMap<>();
        Map<Long, String> numberMap = new HashMap<>();
        Map<Long, String> typeMap = new HashMap<>();
        
        Map<Long, String> typeIdToNameMap = new HashMap<>();
        Map<String, Long> nameToTypeIdMap = new HashMap<>();
        List<EquipmentType> allTypes = equipmentTypeMapper.selectList(new LambdaQueryWrapper<>());
        for (EquipmentType t : allTypes) {
            typeIdToNameMap.put(t.getId(), t.getTypeName());
            nameToTypeIdMap.put(t.getTypeName(), t.getId());
        }
        
        Set<Long> filteredEquipmentIds = new HashSet<>();
        LambdaQueryWrapper<Equipment> allEquipmentQuery = new LambdaQueryWrapper<>();
        List<Equipment> allEquipmentList = equipmentMapper.selectList(allEquipmentQuery);
        for (Equipment e : allEquipmentList) {
            nameMap.put(e.getId(), e.getEquipmentName());
            numberMap.put(e.getId(), e.getEquipmentNumber());
            String typeName = typeIdToNameMap.getOrDefault(e.getEquipmentTypeId(), "");
            typeMap.put(e.getId(), typeName);
        }
        
        if (equipmentType != null && !equipmentType.isEmpty()) {
            Long typeId = nameToTypeIdMap.get(equipmentType);
            if (typeId != null) {
                LambdaQueryWrapper<Equipment> equipmentQuery = new LambdaQueryWrapper<>();
                equipmentQuery.eq(Equipment::getEquipmentTypeId, typeId);
                List<Equipment> equipmentList = equipmentMapper.selectList(equipmentQuery);
                for (Equipment e : equipmentList) {
                    filteredEquipmentIds.add(e.getId());
                }
            }
        }
        
        for (EquipmentReservation r : allReservations) {
            if (r.getEquipmentId() != null) {
                if (equipmentType != null && !equipmentType.isEmpty() && !filteredEquipmentIds.contains(r.getEquipmentId())) {
                    continue;
                }
                countMap.put(r.getEquipmentId(), countMap.getOrDefault(r.getEquipmentId(), 0) + 1);
            }
        }
        
        for (Map.Entry<Long, Integer> entry : countMap.entrySet()) {
            ReservationHotspot hotspot = new ReservationHotspot();
            hotspot.setEquipmentId(entry.getKey());
            hotspot.setEquipmentName(nameMap.getOrDefault(entry.getKey(), "未知设备"));
            hotspot.setEquipmentNumber(numberMap.getOrDefault(entry.getKey(), ""));
            hotspot.setEquipmentType(typeMap.getOrDefault(entry.getKey(), ""));
            hotspot.setReservationCount(entry.getValue());
            hotspots.add(hotspot);
        }
        
        hotspots.sort((a, b) -> b.getReservationCount().compareTo(a.getReservationCount()));
        
        if (limit != null && limit > 0) {
            return hotspots.subList(0, Math.min(hotspots.size(), limit));
        }
        return hotspots;
    }
    
    @Override
    public RepairStatistics getRepairStatistics(String equipmentType, Long equipmentId, Long userId, String role) {
        boolean isAdmin = "admin".equals(role);
        
        RepairStatistics stats = new RepairStatistics();
        
        LambdaQueryWrapper<EquipmentRepair> query = new LambdaQueryWrapper<>();
        
        if (equipmentId != null) {
            query.eq(EquipmentRepair::getEquipmentId, equipmentId);
        } else if (equipmentType != null && !equipmentType.isEmpty()) {
            Set<Long> filteredIds = getFilteredEquipmentIds(equipmentType, null);
            if (!filteredIds.isEmpty()) {
                query.in(EquipmentRepair::getEquipmentId, filteredIds);
            }
        }
        
        if (!isAdmin && userId != null) {
            query.eq(EquipmentRepair::getUserId, userId);
        }
        
        List<EquipmentRepair> repairList = repairMapper.selectList(query);
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
