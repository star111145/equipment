package com.swpu.equipment.equipment.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swpu.equipment.common.util.QrCodeUtil;
import com.swpu.equipment.equipment.entity.Equipment;
import com.swpu.equipment.equipment.entity.EquipmentVO;
import com.swpu.equipment.equipment.mapper.EquipmentMapper;
import com.swpu.equipment.equipment.service.EquipmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.Serializable;
import java.util.List;

@Service
public class EquipmentServiceImpl extends ServiceImpl<EquipmentMapper, Equipment> implements EquipmentService {

    @Transactional
    @Override
    public boolean save(Equipment equipment) {
        boolean saved = super.save(equipment);
        if (saved && equipment.getEquipmentNumber() != null) {
            try {
                String qrcodeContent = equipment.getEquipmentNumber();
                String qrcodeUrl = QrCodeUtil.generateQrCode(qrcodeContent, equipment.getEquipmentNumber());
                
                Equipment updateEquipment = new Equipment();
                updateEquipment.setId(equipment.getId());
                updateEquipment.setQrcodeUrl(qrcodeUrl);
                updateEquipment.setQrcodeContent(qrcodeContent);
                super.updateById(updateEquipment);
            } catch (Exception e) {
                File file = new File(equipment.getQrcodeUrl());
                if (file.exists()) {
                    file.delete();
                }
                throw new RuntimeException("生成二维码失败", e);
            }
        }
        return saved;
    }

    @Transactional
    @Override
    public boolean updateById(Equipment equipment) {
        Equipment oldEquipment = super.getById(equipment.getId());
        boolean updated = super.updateById(equipment);
        if (updated && oldEquipment != null) {
            if (oldEquipment.getQrcodeUrl() != null && !oldEquipment.getQrcodeUrl().equals(equipment.getQrcodeUrl())) {
                File file = new File(oldEquipment.getQrcodeUrl());
                if (file.exists()) {
                    file.delete();
                }
            }
        }
        return updated;
    }

    @Transactional
    @Override
    public boolean removeById(Serializable id) {
        Equipment equipment = super.getById(id);
        boolean removed = super.removeById(id);
        if (removed && equipment != null && equipment.getQrcodeUrl() != null) {
            File file = new File(equipment.getQrcodeUrl());
            if (file.exists()) {
                file.delete();
            }
        }
        return removed;
    }

    @Override
    public Page<EquipmentVO> getEquipmentPage(Page<EquipmentVO> page, String keyword, String equipmentType) {
        return (Page<EquipmentVO>) baseMapper.getEquipmentPage(page, keyword, equipmentType);
    }

    @Override
    public EquipmentVO getEquipmentById(Long id) {
        return baseMapper.getEquipmentById(id);
    }

    @Override
    public List<String> getEquipmentTypes() {
        return baseMapper.getEquipmentTypes();
    }
}
