package com.swpu.equipment.equipment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swpu.equipment.equipment.entity.Equipment;
import com.swpu.equipment.equipment.entity.EquipmentVO;

import java.util.List;

public interface EquipmentService extends IService<Equipment> {
    
    Page<EquipmentVO> getEquipmentPage(Page<EquipmentVO> page, String keyword, String equipmentType);
    
    EquipmentVO getEquipmentById(Long id);
    
    List<String> getEquipmentTypes();
}
