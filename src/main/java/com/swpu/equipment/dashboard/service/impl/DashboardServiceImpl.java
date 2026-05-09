package com.swpu.equipment.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.swpu.equipment.dashboard.entity.*;
import com.swpu.equipment.dashboard.export.DashboardExcelData;
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
                  .eq(EquipmentBorrow::getBorrowStatus, 1)
                  .eq(EquipmentBorrow::getAuditStatus, 1);
        List<EquipmentBorrow> allBorrows = borrowMapper.selectList(borrowQuery);
        
        Set<Long> borrowedEquipmentIds = new HashSet<>();
        for (EquipmentBorrow b : allBorrows) {
            if (b.getEquipmentId() != null) {
                borrowedEquipmentIds.add(b.getEquipmentId());
            }
        }
        
        // 查询维修中的设备（报修状态=1且已审核）
        LambdaQueryWrapper<EquipmentRepair> repairQuery = new LambdaQueryWrapper<>();
        repairQuery.in(EquipmentRepair::getEquipmentId, allEquipmentIds)
                  .eq(EquipmentRepair::getRepairStatus, 1)
                  .eq(EquipmentRepair::getAuditStatus, 1);
        List<EquipmentRepair> allRepairs = repairMapper.selectList(repairQuery);
        
        Set<Long> repairingEquipmentIds = new HashSet<>();
        for (EquipmentRepair r : allRepairs) {
            if (r.getEquipmentId() != null) {
                repairingEquipmentIds.add(r.getEquipmentId());
            }
        }
        
        // 查询借出的设备数量
        Map<Long, Long> borrowedCountMap = new HashMap<>();
        for (EquipmentBorrow b : allBorrows) {
            if (b.getEquipmentId() != null) {
                Long currentBorrowed = borrowedCountMap.getOrDefault(b.getEquipmentId(), 0L);
                borrowedCountMap.put(b.getEquipmentId(), currentBorrowed + (b.getBorrowQuantity() != null ? b.getBorrowQuantity() : 1));
            }
        }
        
        // 查询维修中的设备数量
        Map<Long, Long> repairingCountMap = new HashMap<>();
        for (EquipmentRepair r : allRepairs) {
            if (r.getEquipmentId() != null) {
                Long currentRepairing = repairingCountMap.getOrDefault(r.getEquipmentId(), 0L);
                repairingCountMap.put(r.getEquipmentId(), currentRepairing + (r.getRepairQuantity() != null ? r.getRepairQuantity() : 1));
            }
        }
        
        // 统计设备状态：按实际数量计算（只扣减借用和故障数量）
        for (Equipment e : allEquipment) {
            int stock = e.getStockQuantity() != null ? e.getStockQuantity() : 1;
            long borrowed = borrowedCountMap.getOrDefault(e.getId(), 0L);
            long repairing = repairingCountMap.getOrDefault(e.getId(), 0L);
            
            long available = stock - borrowed - repairing;
            
            statusCount.put("repairing", statusCount.getOrDefault("repairing", 0L) + repairing);
            statusCount.put("borrowed", statusCount.getOrDefault("borrowed", 0L) + borrowed);
            statusCount.put("available", statusCount.getOrDefault("available", 0L) + Math.max(0, available));
        }
        
        return statusCount;
    }
    
    @Override
    public DashboardStatistics getDashboardStatistics(String equipmentType, Long equipmentId, Long userId, String role) {
        DashboardStatistics stats = new DashboardStatistics();
        
        boolean isAdmin = "admin".equals(role);
        
        Set<Long> filteredEquipmentIds = getFilteredEquipmentIds(equipmentType, equipmentId);
        
        // 用户过滤的设备列表（只用于预约/报修统计）
        Set<Long> userFilteredEquipmentIds = new HashSet<>(filteredEquipmentIds);
        Set<Long> userEquipmentIds = new HashSet<>();
        if (!isAdmin && userId != null) {
            userEquipmentIds = getUserEquipmentIds(userId);
            if (equipmentId == null && equipmentType == null) {
                userFilteredEquipmentIds = userEquipmentIds;
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
        List<Equipment> equipmentList = equipmentMapper.selectList(eq);
        
        long totalStock = 0;
        for (Equipment e : equipmentList) {
            totalStock += e.getStockQuantity() != null ? e.getStockQuantity() : 1;
        }
        stats.setTotalEquipment(totalStock);
        
        Map<String, Long> currentStatusCount = calculateCurrentStatusCount(filteredEquipmentIds, null, true);
        
        stats.setAvailableEquipment(currentStatusCount.getOrDefault("available", 0L));
        stats.setReservedEquipment(currentStatusCount.getOrDefault("reserved", 0L));
        stats.setBorrowedEquipment(currentStatusCount.getOrDefault("borrowed", 0L));
        stats.setRepairingEquipment(currentStatusCount.getOrDefault("repairing", 0L));
        stats.setBrokenEquipment(currentStatusCount.getOrDefault("broken", 0L));
        
        LambdaQueryWrapper<EquipmentReservation> rq = new LambdaQueryWrapper<>();
        if (equipmentId != null) {
            rq.eq(EquipmentReservation::getEquipmentId, equipmentId);
        } else if (!userFilteredEquipmentIds.isEmpty()) {
            rq.in(EquipmentReservation::getEquipmentId, userFilteredEquipmentIds);
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
        } else if (!userFilteredEquipmentIds.isEmpty()) {
            bk.in(EquipmentBorrow::getEquipmentId, userFilteredEquipmentIds);
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
        } else if (!userFilteredEquipmentIds.isEmpty()) {
            rp.in(EquipmentRepair::getEquipmentId, userFilteredEquipmentIds);
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
        String[] statusKeys = {"repairing", "available", "borrowed"};
        String[] statusTexts = {"故障", "空闲", "已借用"};
        
        for (int i = 0; i < 3; i++) {
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
                .eq(EquipmentRepair::getAuditStatus, 1)
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
    
    private LocalDateTime getStartTimeByPeriod(String period) {
        LocalDateTime now = LocalDateTime.now();
        if (period == null) {
            return null;
        }
        switch (period) {
            case "week":
                return now.minusDays(7);
            case "month":
                return now.minusDays(30);
            case "semester":
                return now.minusDays(120);
            default:
                return null;
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
    
    @Override
    public List<DashboardExcelData> getExportData(String reportType, String equipmentType, Long equipmentId, Long userId, String role, String period) {
        List<DashboardExcelData> dataList = new ArrayList<>();
        
        Set<Long> equipmentIds = getFilteredEquipmentIds(equipmentType, equipmentId);
        
        LocalDateTime startTime = getStartTimeByPeriod(period);
        
        if ("all".equals(reportType) || "statistics".equals(reportType)) {
            DashboardStatistics statistics = getDashboardStatistics(equipmentType, equipmentId, userId, role);
            
            DashboardExcelData statData = new DashboardExcelData();
            statData.setStatType("概览统计");
            statData.setEquipmentName("全部设备");
            statData.setCount(statistics.getTotalEquipment() != null ? statistics.getTotalEquipment().intValue() : 0);
            statData.setRemark("设备总数");
            dataList.add(statData);
            
            DashboardExcelData availableData = new DashboardExcelData();
            availableData.setStatType("概览统计");
            availableData.setEquipmentName("全部设备");
            availableData.setCount(statistics.getAvailableEquipment() != null ? statistics.getAvailableEquipment().intValue() : 0);
            availableData.setRemark("可用设备数");
            dataList.add(availableData);
            
            DashboardExcelData reservationData = new DashboardExcelData();
            reservationData.setStatType("概览统计");
            reservationData.setEquipmentName("全部设备");
            reservationData.setCount(statistics.getTotalReservation() != null ? statistics.getTotalReservation().intValue() : 0);
            reservationData.setRemark("预约申请总数");
            dataList.add(reservationData);
            
            DashboardExcelData repairData = new DashboardExcelData();
            repairData.setStatType("概览统计");
            repairData.setEquipmentName("全部设备");
            repairData.setCount(statistics.getTotalRepair() != null ? statistics.getTotalRepair().intValue() : 0);
            repairData.setRemark("报修申请总数");
            dataList.add(repairData);
        }
        
        if ("all".equals(reportType) || "equipment_status".equals(reportType)) {
            List<EquipmentStatusCount> statusCounts = getEquipmentStatusCount(equipmentType, equipmentId, userId, role);
            
            for (EquipmentStatusCount status : statusCounts) {
                DashboardExcelData data = new DashboardExcelData();
                data.setStatType("设备状态分布");
                data.setCount(status.getCount() != null ? status.getCount().intValue() : 0);
                data.setRemark(status.getStatusText());
                dataList.add(data);
            }
        }
        
        if ("all".equals(reportType) || "usage_trend".equals(reportType)) {
            List<EquipmentUsageTrend> trends = getEquipmentUsageTrend(period, equipmentType, equipmentId, userId, role);
            
            for (EquipmentUsageTrend trend : trends) {
                DashboardExcelData data = new DashboardExcelData();
                data.setStatType("使用趋势");
                data.setDate(trend.getDate() != null ? trend.getDate().toString() : "");
                data.setCount(trend.getBorrowCount() != null ? trend.getBorrowCount().intValue() : 0);
                data.setRemark("借用次数");
                dataList.add(data);
                
                DashboardExcelData reserveData = new DashboardExcelData();
                reserveData.setStatType("使用趋势");
                reserveData.setDate(trend.getDate() != null ? trend.getDate().toString() : "");
                reserveData.setCount(trend.getReserveCount() != null ? trend.getReserveCount().intValue() : 0);
                reserveData.setRemark("预约次数");
                dataList.add(reserveData);
            }
        }
        
        if ("all".equals(reportType) || "reservation".equals(reportType)) {
            LambdaQueryWrapper<EquipmentReservation> reserveQuery = new LambdaQueryWrapper<>();
            if (equipmentIds != null && !equipmentIds.isEmpty()) {
                reserveQuery.in(EquipmentReservation::getEquipmentId, equipmentIds);
            }
            if (userId != null && !"admin".equals(role)) {
                reserveQuery.eq(EquipmentReservation::getUserId, userId);
            }
            if (startTime != null) {
                reserveQuery.ge(EquipmentReservation::getCreateTime, startTime);
            }
            reserveQuery.orderByDesc(EquipmentReservation::getCreateTime);
            reserveQuery.last("LIMIT 1000");
            List<EquipmentReservation> reservations = reservationMapper.selectList(reserveQuery);
            
            for (EquipmentReservation res : reservations) {
                DashboardExcelData data = new DashboardExcelData();
                data.setStatType("预约记录");
                data.setEquipmentName(res.getEquipmentName());
                data.setEquipmentNumber(res.getEquipmentNumber());
                if (res.getEquipmentTypeId() != null) {
                    EquipmentType type = equipmentTypeMapper.selectById(res.getEquipmentTypeId());
                    data.setEquipmentType(type != null ? type.getTypeName() : "");
                } else {
                    data.setEquipmentType("");
                }
                data.setDate(res.getCreateTime() != null ? res.getCreateTime().toString() : "");
                data.setRemark(getReservationStatusText(res.getReserveStatus()));
                dataList.add(data);
            }
        }
        
        if ("all".equals(reportType) || "borrow".equals(reportType)) {
            List<EquipmentBorrow> borrows = borrowMapper.selectListWithDetails(equipmentIds, "admin".equals(role) ? null : userId, startTime);
            
            for (EquipmentBorrow borrow : borrows) {
                DashboardExcelData data = new DashboardExcelData();
                data.setStatType("借用记录");
                data.setEquipmentName(borrow.getEquipmentName());
                data.setEquipmentNumber(borrow.getEquipmentNumber());
                data.setEquipmentType(borrow.getEquipmentType());
                Integer qty = borrow.getOriginalQuantity() != null ? borrow.getOriginalQuantity() : borrow.getBorrowQuantity();
                data.setCount(qty != null ? qty : 1);
                data.setDate(borrow.getCreateTime() != null ? borrow.getCreateTime().toString() : "");
                data.setRemark(getBorrowStatusText(borrow.getBorrowStatus()));
                dataList.add(data);
            }
        }
        
        if ("all".equals(reportType) || "repair".equals(reportType)) {
            LambdaQueryWrapper<EquipmentRepair> repairQuery = new LambdaQueryWrapper<>();
            if (equipmentIds != null && !equipmentIds.isEmpty()) {
                repairQuery.in(EquipmentRepair::getEquipmentId, equipmentIds);
            }
            if (userId != null && !"admin".equals(role)) {
                repairQuery.eq(EquipmentRepair::getUserId, userId);
            }
            if (startTime != null) {
                repairQuery.ge(EquipmentRepair::getCreateTime, startTime);
            }
            repairQuery.orderByDesc(EquipmentRepair::getCreateTime);
            repairQuery.last("LIMIT 1000");
            List<EquipmentRepair> repairs = repairMapper.selectList(repairQuery);
            
            for (EquipmentRepair repair : repairs) {
                DashboardExcelData data = new DashboardExcelData();
                data.setStatType("报修记录");
                data.setEquipmentName(repair.getEquipmentName());
                data.setEquipmentNumber(repair.getEquipmentNumber());
                if (repair.getEquipmentTypeName() == null && repair.getEquipmentTypeId() != null) {
                    EquipmentType type = equipmentTypeMapper.selectById(repair.getEquipmentTypeId());
                    data.setEquipmentType(type != null ? type.getTypeName() : "");
                } else {
                    data.setEquipmentType(repair.getEquipmentTypeName());
                }
                data.setCount(repair.getRepairQuantity() != null ? repair.getRepairQuantity() : 1);
                data.setDate(repair.getCreateTime() != null ? repair.getCreateTime().toString() : "");
                data.setRemark(getRepairStatusText(repair.getRepairStatus()));
                dataList.add(data);
            }
        }
        
        if ("all".equals(reportType) || "return".equals(reportType)) {
            List<EquipmentReturn> returns = returnMapper.selectListWithDetails(equipmentIds, "admin".equals(role) ? null : userId, startTime);
            
            for (EquipmentReturn ret : returns) {
                DashboardExcelData data = new DashboardExcelData();
                data.setStatType("归还记录");
                data.setEquipmentName(ret.getEquipmentName());
                data.setEquipmentNumber(ret.getEquipmentNumber());
                data.setEquipmentType(ret.getEquipmentType());
                data.setCount(ret.getReturnQuantity() != null ? ret.getReturnQuantity() : 1);
                data.setDate(ret.getCreateTime() != null ? ret.getCreateTime().toString() : "");
                data.setRemark(getReturnStatusText(ret.getReturnStatus()));
                dataList.add(data);
            }
        }
        
        return dataList;
    }
}
