package com.swpu.equipment.lifecycle.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swpu.equipment.lifecycle.entity.EquipmentRepair;

public interface EquipmentRepairService extends IService<EquipmentRepair> {
    boolean approveRepair(Long id, String comment, Integer status, Long auditUserId, String auditUserName);
    
    boolean completeRepair(Long id, String comment);
    
    boolean cancelRepair(Long id);

    IPage<EquipmentRepair> getPageListWithType(Page<EquipmentRepair> page, Long userId, Integer repairStatus, Integer auditStatus, String keyword);
    
    Long getUserPendingRepairQuantity(Long equipmentId, Long userId);
}
