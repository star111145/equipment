package com.swpu.equipment.warehouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swpu.equipment.warehouse.entity.Warehouse;
import com.swpu.equipment.warehouse.entity.WarehouseVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;



@Mapper
public interface WarehouseMapper extends BaseMapper<Warehouse> {
    
    IPage<WarehouseVO> getWarehouseList(Page<WarehouseVO> page, @Param("keyword") String keyword);
     
}
