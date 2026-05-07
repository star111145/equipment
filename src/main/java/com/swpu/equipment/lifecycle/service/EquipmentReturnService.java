package com.swpu.equipment.lifecycle.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swpu.equipment.lifecycle.entity.EquipmentReturn;

public interface EquipmentReturnService extends IService<EquipmentReturn> {
    IPage<EquipmentReturn> getPageListWithType(Page<EquipmentReturn> page, Long userId, Integer status, Integer auditStatus, String keyword);
}
