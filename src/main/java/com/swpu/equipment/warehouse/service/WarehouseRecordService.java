package com.swpu.equipment.warehouse.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swpu.equipment.warehouse.entity.WarehouseRecord;
import com.swpu.equipment.warehouse.entity.WarehouseRecordVO;

public interface WarehouseRecordService extends IService<WarehouseRecord> {
    
    IPage<WarehouseRecordVO> getRecordPage(Page<WarehouseRecordVO> page, Long equipmentId, Long warehouseId, Long supplierId, Integer recordType, String keyword);
}
