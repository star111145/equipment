package com.swpu.equipment.warehouse.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swpu.equipment.warehouse.entity.Warehouse;
import com.swpu.equipment.warehouse.entity.WarehouseVO;
import com.swpu.equipment.warehouse.mapper.WarehouseMapper;
import com.swpu.equipment.warehouse.service.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class WarehouseServiceImpl extends ServiceImpl<WarehouseMapper, Warehouse> implements WarehouseService {

    @Autowired
    private WarehouseMapper warehouseMapper;

    @Override
    public IPage<WarehouseVO> getWarehousePage(Page<WarehouseVO> page, String keyword) {
        return warehouseMapper.getWarehouseList(page, keyword);
    }

    @Override
    public List<Map<String, Object>> getManagerOptions() {
        return warehouseMapper.getManagerOptions();
    }
}
