package com.swpu.equipment.equipment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swpu.equipment.equipment.entity.Equipment;
import com.swpu.equipment.equipment.entity.EquipmentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EquipmentMapper extends BaseMapper<Equipment> {
    IPage<EquipmentVO> getEquipmentPage(Page<EquipmentVO> page, @Param("keyword") String keyword, @Param("equipmentType") String equipmentType);
    
    EquipmentVO getEquipmentById(@Param("id") Long id);
    
    List<String> getEquipmentTypes();
}
