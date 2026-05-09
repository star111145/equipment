package com.swpu.equipment.lifecycle.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swpu.equipment.lifecycle.entity.EquipmentReservation;
import com.swpu.equipment.lifecycle.entity.EquipmentReservationVO;

import java.time.LocalDateTime;
import java.util.List;

public interface EquipmentReservationService extends IService<EquipmentReservation> {
    
    IPage<EquipmentReservationVO> getPageList(Page<EquipmentReservationVO> page, String keyword, Integer status, Integer auditStatus);
    
    IPage<EquipmentReservationVO> getPageListByUserId(Page<EquipmentReservationVO> page, Long userId, String keyword, Integer status);
    
    EquipmentReservationVO getReservationById(Long id);
    
    boolean createReservation(EquipmentReservation reservation);
    
    boolean updateReservation(EquipmentReservation reservation);
    
    boolean deleteReservation(Long id);
    
    boolean approveReservation(Long id, Long auditUserId, String comment, Integer status);
    
    boolean checkConflict(Long equipmentId, LocalDateTime reserveTime, Integer reserveDuration, Long excludeId);
    
    boolean hasPendingReservation(Long userId, Long equipmentId);
    
    boolean hasActiveReservation(Long userId, Long equipmentId);
    
    boolean hasOverdueBorrow(Long userId);
    
    List<EquipmentReservationVO> getReservationsForCalendar(Long equipmentId, Long equipmentTypeId, LocalDateTime start, LocalDateTime end);
    
    List<EquipmentReservationVO> getExportList(String keyword, Integer status, Integer auditStatus, Boolean exportAll, Integer current, Integer size);
}
