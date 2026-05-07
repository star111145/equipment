package com.swpu.equipment.lifecycle.service.impl;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swpu.equipment.equipment.entity.Equipment;
import com.swpu.equipment.equipment.mapper.EquipmentMapper;
import com.swpu.equipment.equipment.service.EquipmentService;
import com.swpu.equipment.lifecycle.entity.EquipmentBorrow;
import com.swpu.equipment.lifecycle.mapper.EquipmentBorrowMapper;
import com.swpu.equipment.lifecycle.mapper.EquipmentRepairMapper;
import com.swpu.equipment.lifecycle.mapper.EquipmentReturnMapper;
import com.swpu.equipment.lifecycle.service.EquipmentBorrowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



@Service
public class EquipmentBorrowServiceImpl extends ServiceImpl<EquipmentBorrowMapper, EquipmentBorrow> implements EquipmentBorrowService {

    @Autowired
    private EquipmentMapper equipmentMapper;
    
    @Autowired
    private EquipmentService equipmentService;
    
    @Autowired
    private EquipmentRepairMapper repairMapper;
    
    @Autowired
    private EquipmentReturnMapper returnMapper;
    

    @Transactional
    @Override
    public boolean approveBorrow(EquipmentBorrow borrow, String comment, Integer status) {
        if (borrow == null) {
            return false;
        }
        
        if (status == 1) { // 同意借用
            borrow.setBorrowStatus(1); // 已借出
            
            Equipment equipment = equipmentMapper.selectById(borrow.getEquipmentId());
            Integer newAvailable = equipment.getAvailableQuantity() - borrow.getBorrowQuantity();
            equipment.setAvailableQuantity(Math.max(0, newAvailable));
            equipmentMapper.updateById(equipment);
            
            equipmentService.updateEquipmentStatus(borrow.getEquipmentId());
        } else if (status == 2) { // 拒绝借用
            borrow.setBorrowStatus(4); // 已拒绝
        }
        
        borrow.setAuditStatus(1);
        borrow.setAuditResult(comment);
        baseMapper.updateById(borrow);
        
        return true;
    }

    @Transactional
    @Override
    public boolean returnEquipment(Long id, Integer returnQuantity) {
        EquipmentBorrow borrow = baseMapper.selectById(id);
        if (borrow == null) {
            return false;
        }
        
        Integer newQuantity = borrow.getBorrowQuantity() - returnQuantity;
        if (newQuantity <= 0) {
            borrow.setBorrowStatus(2);
            borrow.setBorrowQuantity(0);
        } else {
            borrow.setBorrowQuantity(newQuantity);
        }
        
        boolean success = baseMapper.updateById(borrow) > 0;
        if (!success) {
            return false;
        }
        
        Equipment equipment = equipmentMapper.selectById(borrow.getEquipmentId());
        Integer newAvailable = equipment.getAvailableQuantity() + returnQuantity;
        equipment.setAvailableQuantity(Math.min(equipment.getStockQuantity(), newAvailable));
        equipmentMapper.updateById(equipment);
        
        equipmentService.updateEquipmentStatus(borrow.getEquipmentId());
        
        return true;
    }
    
    @Override
    public Long getRepairQuantity(Long equipmentId) {
        return baseMapper.getRepairQuantity(equipmentId);
    }
    
    @Override
    public Long getRepairQuantityByUser(Long equipmentId, Long userId) {
        return baseMapper.getRepairQuantityByUser(equipmentId, userId);
    }
    
    @Override
    public Long getUserUnreturnedBorrowQuantity(Long equipmentId, Long userId) {
        Long quantity = baseMapper.getUserUnreturnedBorrowQuantity(equipmentId, userId);
        return quantity != null ? quantity : 0L;
    }
    
    @Override
    public IPage<EquipmentBorrow> getPageListWithType(Page<EquipmentBorrow> page, Long userId, Integer status, Integer auditStatus, String keyword) {
        return baseMapper.getPageListWithType(page, userId, status, auditStatus, keyword);
    }
    
    @Override
    public boolean hasOverdueBorrow(Long userId) {
        int count = baseMapper.countOverdueBorrows(userId);
        return count > 0;
    }
    
    @Override
    public boolean hasUnreturnedBorrow(Long userId, Long equipmentId) {
        return baseMapper.countUnreturnedBorrows(userId, equipmentId) > 0;
    }
    
    @Override
    public boolean hasPendingApplication(Long userId, Long equipmentId) {
        int pendingBorrow = baseMapper.countPendingBorrows(userId, equipmentId);
        if (pendingBorrow > 0) {
            return true;
        }
        
        int pendingReturn = returnMapper.countPendingReturns(userId, equipmentId);
        if (pendingReturn > 0) {
            return true;
        }
        
        int pendingRepair = repairMapper.countPendingRepairs(userId, equipmentId);
        return pendingRepair > 0;
    }
}
