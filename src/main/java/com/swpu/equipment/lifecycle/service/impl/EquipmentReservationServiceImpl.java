package com.swpu.equipment.lifecycle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swpu.equipment.equipment.entity.Equipment;
import com.swpu.equipment.equipment.mapper.EquipmentMapper;
import com.swpu.equipment.lifecycle.entity.EquipmentReservation;
import com.swpu.equipment.lifecycle.entity.EquipmentReservationVO;
import com.swpu.equipment.lifecycle.mapper.EquipmentBorrowMapper;
import com.swpu.equipment.lifecycle.mapper.EquipmentReservationMapper;
import com.swpu.equipment.lifecycle.service.EquipmentReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EquipmentReservationServiceImpl extends ServiceImpl<EquipmentReservationMapper, EquipmentReservation> implements EquipmentReservationService {
    
    @Autowired
    private EquipmentMapper equipmentMapper;
    
    @Autowired
    private EquipmentBorrowMapper borrowMapper;
    
    @Override
    public IPage<EquipmentReservationVO> getPageList(Page<EquipmentReservationVO> page, String keyword, Integer status) {
        return baseMapper.getPageList(page, keyword, status);
    }
    
    @Override
    public IPage<EquipmentReservationVO> getPageListByUserId(Page<EquipmentReservationVO> page, Long userId, String keyword, Integer status) {
        return baseMapper.getPageListByUserId(page, userId, keyword, status);
    }
    
    @Override
    public EquipmentReservationVO getReservationById(Long id) {
        return baseMapper.getReservationById(id);
    }
    
    @Transactional
    @Override
    public boolean createReservation(EquipmentReservation reservation) {
        Equipment equipment = equipmentMapper.selectById(reservation.getEquipmentId());
        if (equipment != null) {
            reservation.setEquipmentNumber(equipment.getEquipmentNumber());
            reservation.setEquipmentName(equipment.getEquipmentName());
            reservation.setEquipmentModel(equipment.getEquipmentModel());
            reservation.setEquipmentImage(equipment.getEquipmentImage());
        }
        
        reservation.setReserveStatus(0);
        reservation.setAuditStatus(0);
        reservation.setCreateTime(LocalDateTime.now());
        reservation.setUpdateTime(LocalDateTime.now());
        return save(reservation);
    }
    
    @Transactional
    @Override
    public boolean updateReservation(EquipmentReservation reservation) {
        reservation.setUpdateTime(LocalDateTime.now());
        return updateById(reservation);
    }
    
    @Transactional
    @Override
    public boolean deleteReservation(Long id) {
        return removeById(id);
    }
    
    @Transactional
    @Override
    public boolean approveReservation(Long id, Long auditUserId, String comment, Integer status) {
        EquipmentReservation reservation = getById(id);
        if (reservation == null) {
            return false;
        }
        
        // 如果是审核通过（status=1），需要检测冲突
        if (status == 1) {
            boolean hasConflict = checkConflict(
                reservation.getEquipmentId(),
                reservation.getReserveTime(),
                reservation.getReserveDuration(),
                id  // 排除自身
            );
            if (hasConflict) {
                throw new RuntimeException("该预约时段存在冲突，无法通过审核");
            }
        }
        
        reservation.setAuditStatus(status);
        reservation.setAuditResult(comment);
        reservation.setAuditUserId(auditUserId);
        reservation.setAuditTime(LocalDateTime.now());
        reservation.setReserveStatus(status == 1 ? 1 : 2);
        reservation.setUpdateTime(LocalDateTime.now());
        
        return updateById(reservation);
    }
    
    /**
     * 预约冲突检测算法
     * 
     * 算法逻辑：
     *   新预约A：[startA, endA]
     *   已通过预约B：[startB, endB]
     *   冲突条件：startA < endB AND endA > startB
     * 
     * 检测范围：仅检测同一设备、同一时段内是否存在"已通过"（status=1）状态的预约记录
     * 说明：待审核（status=0）的预约不参与冲突检测，允许与已通过预约时间重叠，
     *       由管理员在审核时判断哪个可以通过
     * 
     * @param equipmentId 设备ID
     * @param reserveTime 预约开始时间
     * @param reserveDuration 预约时长（小时）
     * @param excludeId 排除的预约ID（修改时使用，避免与自身冲突）
     * @return true-存在冲突，false-无冲突
     */
    @Override
    public boolean checkConflict(Long equipmentId, LocalDateTime reserveTime, Integer reserveDuration, Long excludeId) {
        // 计算预约结束时间
        LocalDateTime endTime = reserveTime.plusHours(reserveDuration.longValue());
        
        List<EquipmentReservation> list;
        if (excludeId != null) {
            // 修改预约时，排除自身记录
            list = baseMapper.selectList(
                new LambdaQueryWrapper<EquipmentReservation>()
                    .eq(EquipmentReservation::getEquipmentId, equipmentId)
                    .ne(EquipmentReservation::getId, excludeId)
                    // 只检测"已通过"（status=1）状态的预约
                    // 待审核（status=0）的预约不参与冲突检测，允许重叠
                    .eq(EquipmentReservation::getReserveStatus, 1)
                    .and(wrapper -> wrapper
                        // 场景1：新预约的开始时间在已有预约时段内
                        // startA >= startB AND startA < endB
                        .and(w -> w
                            .ge(EquipmentReservation::getReserveTime, reserveTime)
                            .lt(EquipmentReservation::getReserveTime, endTime)
                        )
                        // 场景2：新预约覆盖已有预约（包含关系）
                        // startA <= startB AND endA > endB
                        .or(w -> w
                            .le(EquipmentReservation::getReserveTime, reserveTime)
                            .ge(EquipmentReservation::getReserveTime, endTime)
                        )
                    )
            );
        } else {
            // 新增预约时
            list = baseMapper.selectList(
                new LambdaQueryWrapper<EquipmentReservation>()
                    .eq(EquipmentReservation::getEquipmentId, equipmentId)
                    // 只检测"已通过"（status=1）状态的预约
                    // 待审核（status=0）的预约不参与冲突检测
                    .eq(EquipmentReservation::getReserveStatus, 1)
                    .and(wrapper -> wrapper
                        // 场景1：新预约的开始时间在已有预约时段内
                        .and(w -> w
                            .ge(EquipmentReservation::getReserveTime, reserveTime)
                            .lt(EquipmentReservation::getReserveTime, endTime)
                        )
                        // 场景2：新预约覆盖已有预约
                        .or(w -> w
                            .le(EquipmentReservation::getReserveTime, reserveTime)
                            .ge(EquipmentReservation::getReserveTime, endTime)
                        )
                    )
            );
        }
        
        // 存在冲突记录返回true，否则返回false
        return !list.isEmpty();
    }

    @Override
    public boolean hasPendingReservation(Long userId, Long equipmentId) {
        Long count = baseMapper.selectCount(
            new LambdaQueryWrapper<EquipmentReservation>()
                .eq(EquipmentReservation::getUserId, userId)
                .eq(EquipmentReservation::getEquipmentId, equipmentId)
                .eq(EquipmentReservation::getReserveStatus, 0)
        );
        return count != null && count > 0;
    }
    
    @Override
    public boolean hasActiveReservation(Long userId, Long equipmentId) {
        // 检查是否有已通过且未过期的预约
        List<EquipmentReservation> list = baseMapper.selectList(
            new LambdaQueryWrapper<EquipmentReservation>()
                .eq(EquipmentReservation::getUserId, userId)
                .eq(EquipmentReservation::getEquipmentId, equipmentId)
                .eq(EquipmentReservation::getReserveStatus, 1)
        );
        
        LocalDateTime now = LocalDateTime.now();
        for (EquipmentReservation r : list) {
            LocalDateTime endTime = r.getReserveTime().plusHours(r.getReserveDuration().longValue());
            if (endTime.isAfter(now)) {
                return true;
            }
        }
        
        // 检查是否有未归还的借用
        Long borrowCount = borrowMapper.countUnreturnedBorrows(userId, equipmentId);
        return borrowCount != null && borrowCount > 0;
    }
    
    @Override
    public boolean hasOverdueBorrow(Long userId) {
        int count = borrowMapper.countOverdueBorrows(userId);
        return count > 0;
    }
    
    @Override
    public List<EquipmentReservationVO> getReservationsForCalendar(Long equipmentId, LocalDateTime start, LocalDateTime end) {
        return baseMapper.getCalendarReservations(equipmentId, start, end);
    }
}
