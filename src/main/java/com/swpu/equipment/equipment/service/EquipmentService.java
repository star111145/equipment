package com.swpu.equipment.equipment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swpu.equipment.equipment.entity.Equipment;
import com.swpu.equipment.equipment.entity.EquipmentVO;
import com.swpu.equipment.equipment.export.EquipmentExcelData;
import com.swpu.equipment.lifecycle.entity.EquipmentBorrow;

import java.util.List;

public interface EquipmentService extends IService<Equipment> {
    
    Page<EquipmentVO> getEquipmentPage(Page<EquipmentVO> page, String keyword, String equipmentType);
    
    EquipmentVO getEquipmentById(Long id);
    
    List<String> getEquipmentTypes();
    
    List<EquipmentExcelData> exportEquipmentList();
    
    List<EquipmentExcelData> exportSelectedEquipment(List<Long> equipmentIds);
    
    void calculateEquipmentStatus(Equipment equipment);
    
    boolean updateEquipmentStatus(Long equipmentId);
    
    boolean borrowEquipment(EquipmentBorrow borrow);
    
    boolean returnEquipment(Long borrowId);
    
    boolean approveRepair(Long repairId, String comment, Integer status);
}
