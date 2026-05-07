package com.swpu.equipment.lifecycle.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swpu.equipment.lifecycle.entity.EquipmentBorrow;

public interface EquipmentBorrowService extends IService<EquipmentBorrow> {
    boolean approveBorrow(EquipmentBorrow borrow, String comment, Integer status);
    
    boolean returnEquipment(Long id, Integer returnQuantity);
    
    Long getRepairQuantity(Long equipmentId);
    
    Long getRepairQuantityByUser(Long equipmentId, Long userId);
    
    Long getUserUnreturnedBorrowQuantity(Long equipmentId, Long userId);
    
    IPage<EquipmentBorrow> getPageListWithType(Page<EquipmentBorrow> page, Long userId, Integer status, Integer auditStatus, String keyword);
    
    boolean hasOverdueBorrow(Long userId);
    
    boolean hasUnreturnedBorrow(Long userId, Long equipmentId);
    
    boolean hasPendingApplication(Long userId, Long equipmentId);
}
