package com.swpu.equipment.warehouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swpu.equipment.warehouse.entity.WarehouseRecord;
import com.swpu.equipment.warehouse.entity.WarehouseRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WarehouseRecordMapper extends BaseMapper<WarehouseRecord> {
    
    IPage<WarehouseRecordVO> getRecordPage(Page<WarehouseRecordVO> page, 
            @Param("equipmentId") Long equipmentId, 
            @Param("warehouseId") Long warehouseId, 
            @Param("supplierId") Long supplierId, 
            @Param("recordType") Integer recordType,
            @Param("keyword") String keyword);
    
    List<WarehouseRecordVO> getRecordList(
            @Param("warehouseId") Long warehouseId, 
            @Param("recordType") Integer recordType,
            @Param("keyword") String keyword);
}
