package com.swpu.equipment.lifecycle.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swpu.equipment.lifecycle.entity.EquipmentReturn;
import com.swpu.equipment.lifecycle.mapper.EquipmentReturnMapper;
import com.swpu.equipment.lifecycle.service.EquipmentReturnService;
import org.springframework.stereotype.Service;

@Service
public class EquipmentReturnServiceImpl extends ServiceImpl<EquipmentReturnMapper, EquipmentReturn> implements EquipmentReturnService {
    
    @Override
    public IPage<EquipmentReturn> getPageListWithType(Page<EquipmentReturn> page, Long userId, Integer status, Integer auditStatus, String keyword) {
        return baseMapper.getPageListWithType(page, userId, status, auditStatus, keyword);
    }
}
