package com.swpu.equipment.equipment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swpu.equipment.equipment.entity.EquipmentType;
import com.swpu.equipment.equipment.mapper.EquipmentTypeMapper;
import com.swpu.equipment.equipment.service.EquipmentTypeService;
import org.springframework.stereotype.Service;

@Service
public class EquipmentTypeServiceImpl extends ServiceImpl<EquipmentTypeMapper, EquipmentType> implements EquipmentTypeService {
}
