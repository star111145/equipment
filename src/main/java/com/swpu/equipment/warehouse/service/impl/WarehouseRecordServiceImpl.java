package com.swpu.equipment.warehouse.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swpu.equipment.warehouse.entity.WarehouseRecord;
import com.swpu.equipment.warehouse.entity.WarehouseRecordVO;
import com.swpu.equipment.warehouse.mapper.WarehouseRecordMapper;
import com.swpu.equipment.warehouse.service.WarehouseRecordService;
import org.springframework.stereotype.Service;

@Service
public class WarehouseRecordServiceImpl extends ServiceImpl<WarehouseRecordMapper, WarehouseRecord> implements WarehouseRecordService {

    @Override
    public IPage<WarehouseRecordVO> getRecordPage(Page<WarehouseRecordVO> page, Long equipmentId, Long warehouseId, Long supplierId, Integer recordType, String keyword) {
        return baseMapper.getRecordPage(page, equipmentId, warehouseId, supplierId, recordType, keyword);
    }
}
