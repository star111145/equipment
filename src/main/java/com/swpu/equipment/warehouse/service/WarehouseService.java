package com.swpu.equipment.warehouse.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swpu.equipment.warehouse.entity.Warehouse;
import com.swpu.equipment.warehouse.entity.WarehouseVO;


public interface WarehouseService extends IService<Warehouse> {
    
    IPage<WarehouseVO> getWarehousePage(Page<WarehouseVO> page, String keyword);
    
}
