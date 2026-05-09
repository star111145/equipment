package com.swpu.equipment.equipment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swpu.equipment.common.util.QrCodeUtil;
import com.swpu.equipment.equipment.entity.Equipment;
import com.swpu.equipment.equipment.entity.EquipmentVO;
import com.swpu.equipment.equipment.export.EquipmentExcelData;
import com.swpu.equipment.equipment.mapper.EquipmentMapper;
import com.swpu.equipment.equipment.service.EquipmentService;
import com.swpu.equipment.lifecycle.entity.EquipmentBorrow;
import com.swpu.equipment.lifecycle.entity.EquipmentRepair;
import com.swpu.equipment.lifecycle.mapper.EquipmentBorrowMapper;
import com.swpu.equipment.lifecycle.mapper.EquipmentRepairMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Service
public class EquipmentServiceImpl extends ServiceImpl<EquipmentMapper, Equipment> implements EquipmentService {
    
    private static final Logger log = LoggerFactory.getLogger(EquipmentServiceImpl.class);
    
    @Value("${mobile.base-url:http://localhost:5173}")
    private String mobileBaseUrl;
    
    @Autowired
    private EquipmentBorrowMapper borrowMapper;
    
    @Autowired
    private EquipmentRepairMapper repairMapper;
    
    @Autowired
    private com.swpu.equipment.equipment.service.EquipmentTypeService equipmentTypeService;
    
    @Autowired
    private com.swpu.equipment.supplier.service.SupplierService supplierService;
    
    @Autowired
    private com.swpu.equipment.warehouse.service.WarehouseService warehouseService;

    @Transactional
    @Override
    public boolean save(Equipment equipment) {
        boolean saved = super.save(equipment);
        if (saved && equipment.getEquipmentNumber() != null) {
            try {
                String qrcodeContent = mobileBaseUrl + "/mobile/device?equipmentNumber=" + equipment.getEquipmentNumber();
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
    public EquipmentVO getEquipmentByNumber(String equipmentNumber) {
        return baseMapper.getEquipmentByNumber(equipmentNumber);
    }

    @Override
    public List<String> getEquipmentTypes() {
        return baseMapper.getEquipmentTypes();
    }

    @Override
    public List<EquipmentExcelData> exportEquipmentList() {
        return exportEquipmentList(null, null, false, 1, Integer.MAX_VALUE);
    }

    @Override
    public String getMaxEquipmentNumber() {
        return baseMapper.getMaxEquipmentNumber();
    }
    
    @Override
    public List<EquipmentExcelData> exportEquipmentList(String keyword, String equipmentType, Boolean exportAll, Integer current, Integer size) {
        if (exportAll == null) {
            exportAll = false;
        }
        int querySize = exportAll ? 100000 : size;
        
        LambdaQueryWrapper<Equipment> wrapper = new LambdaQueryWrapper<>();
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w
                .like(Equipment::getEquipmentNumber, keyword)
                .or()
                .like(Equipment::getEquipmentName, keyword)
            );
        }
        
        if (equipmentType != null && !equipmentType.trim().isEmpty()) {
            wrapper.eq(Equipment::getEquipmentType, equipmentType);
        }
        
        wrapper.last("ORDER BY id LIMIT " + querySize);
        
        List<Equipment> equipmentList = baseMapper.selectList(wrapper);
        List<EquipmentExcelData> excelDataList = new ArrayList<>();
        
        for (Equipment equipment : equipmentList) {
            EquipmentExcelData data = new EquipmentExcelData();
            data.setEquipmentNumber(equipment.getEquipmentNumber());
            data.setEquipmentName(equipment.getEquipmentName());
            
            String typeName = "未设置";
            if (equipment.getEquipmentTypeId() != null) {
                com.swpu.equipment.equipment.entity.EquipmentType type = equipmentTypeService.getById(equipment.getEquipmentTypeId());
                if (type != null) {
                    typeName = type.getTypeName();
                }
            }
            data.setEquipmentType(typeName);
            
            String statusText = "未知";
            switch (equipment.getEquipmentStatus()) {
                case 0: statusText = "维修中"; break;
                case 1: statusText = "空闲"; break;
                case 2: statusText = "被预约"; break;
                case 3: statusText = "已借用"; break;
                case 4: statusText = "故障"; break;
            }
            data.setEquipmentStatus(statusText);
            
            String warehouseName = "未设置";
            String warehouseLocation = "未设置";
            if (equipment.getWarehouseId() != null) {
                com.swpu.equipment.warehouse.entity.Warehouse warehouse = warehouseService.getById(equipment.getWarehouseId());
                if (warehouse != null) {
                    warehouseName = warehouse.getWarehouseName() != null ? warehouse.getWarehouseName() : "未设置";
                    warehouseLocation = warehouse.getWarehouseLocation() != null ? warehouse.getWarehouseLocation() : "未设置";
                }
            }
            data.setWarehouseName(warehouseName);
            data.setWarehouseLocation(warehouseLocation);
            
            data.setStockQuantity(equipment.getStockQuantity() != null ? equipment.getStockQuantity() : 0);
            
            String supplierName = "未设置";
            if (equipment.getSupplierId() != null) {
                com.swpu.equipment.supplier.entity.Supplier supplier = supplierService.getById(equipment.getSupplierId());
                if (supplier != null) {
                    supplierName = supplier.getSupplierName();
                }
            }
            data.setSupplier(supplierName);
            data.setDescription(equipment.getDescription() != null ? equipment.getDescription() : "无");
            
            excelDataList.add(data);
        }
        
        return excelDataList;
    }

    @Override
    public List<EquipmentExcelData> exportSelectedEquipment(List<Long> equipmentIds) {
        List<Equipment> equipmentList = baseMapper.selectBatchIds(equipmentIds);
        List<EquipmentExcelData> excelDataList = new ArrayList<>();
        
        for (Equipment equipment : equipmentList) {
            EquipmentExcelData data = new EquipmentExcelData();
            data.setEquipmentNumber(equipment.getEquipmentNumber());
            data.setEquipmentName(equipment.getEquipmentName());
            
            String typeName = "未设置";
            if (equipment.getEquipmentTypeId() != null) {
                com.swpu.equipment.equipment.entity.EquipmentType type = equipmentTypeService.getById(equipment.getEquipmentTypeId());
                if (type != null) {
                    typeName = type.getTypeName();
                }
            }
            data.setEquipmentType(typeName);
            
            String statusText = "未知";
            switch (equipment.getEquipmentStatus()) {
                case 0: statusText = "维修中"; break;
                case 1: statusText = "空闲"; break;
                case 2: statusText = "被预约"; break;
                case 3: statusText = "已借用"; break;
                case 4: statusText = "故障"; break;
            }
            data.setEquipmentStatus(statusText);
            
            String warehouseName = "未设置";
            String warehouseLocation = "未设置";
            if (equipment.getWarehouseId() != null) {
                com.swpu.equipment.warehouse.entity.Warehouse warehouse = warehouseService.getById(equipment.getWarehouseId());
                if (warehouse != null) {
                    warehouseName = warehouse.getWarehouseName() != null ? warehouse.getWarehouseName() : "未设置";
                    warehouseLocation = warehouse.getWarehouseLocation() != null ? warehouse.getWarehouseLocation() : "未设置";
                }
            }
            data.setWarehouseName(warehouseName);
            data.setWarehouseLocation(warehouseLocation);
            
            data.setStockQuantity(equipment.getStockQuantity() != null ? equipment.getStockQuantity() : 0);
            
            String supplierName = "未设置";
            if (equipment.getSupplierId() != null) {
                com.swpu.equipment.supplier.entity.Supplier supplier = supplierService.getById(equipment.getSupplierId());
                if (supplier != null) {
                    supplierName = supplier.getSupplierName();
                }
            }
            data.setSupplier(supplierName);
            data.setDescription(equipment.getDescription() != null ? equipment.getDescription() : "无");
            
            excelDataList.add(data);
        }
        
        return excelDataList;
    }

    @Override
    public void calculateEquipmentStatus(Equipment equipment) {
        Integer availableQuantity = equipment.getAvailableQuantity() != null ? equipment.getAvailableQuantity() : 0;
        
        Long borrowCount = borrowMapper.getBorrowQuantity(equipment.getId());
        Long repairCount = repairMapper.getRepairQuantity(equipment.getId());
        Long maintenanceCount = repairMapper.getMaintenanceQuantity(equipment.getId());
        Boolean hasActiveReservation = baseMapper.hasActiveReservation(equipment.getId());
        
        if (repairCount > 0 && maintenanceCount == 0) {
            equipment.setEquipmentStatus(4); // 故障
        } else if (repairCount > 0 && maintenanceCount > 0) {
            equipment.setEquipmentStatus(0); // 维修中
        } else if (borrowCount != null && borrowCount > 0 && availableQuantity == 0) {
            equipment.setEquipmentStatus(3); // 已借用
        } else if (hasActiveReservation != null && hasActiveReservation && availableQuantity > 0) {
            equipment.setEquipmentStatus(2); // 被预约
        } else if (availableQuantity > 0) {
            equipment.setEquipmentStatus(1); // 空闲
        } else {
            equipment.setEquipmentStatus(1); // 空闲
        }
    }

    @Override
    @Transactional
    public boolean updateEquipmentStatus(Long equipmentId) {
        Equipment equipment = getById(equipmentId);
        if (equipment == null) {
            return false;
        }
        
        Integer borrowed = baseMapper.selectSumBorrowed(equipmentId);
        Integer returned = baseMapper.selectSumReturned(equipmentId);
        Integer faulty = baseMapper.selectSumFaulty(equipmentId);
        
        log.info("【设备状态】equipmentId=" + equipmentId + ", stock=" + equipment.getStockQuantity() + 
            ", borrowed=" + borrowed + ", returned=" + returned + ", faulty=" + faulty);
        
        Integer availableQuantity = equipment.getStockQuantity() - borrowed - faulty;
        
        Boolean hasPendingRepair = baseMapper.selectHasPendingRepair(equipmentId);
        Boolean hasActiveReservation = baseMapper.hasActiveReservation(equipmentId);
        
        log.info("【设备状态】available=" + availableQuantity + ", hasPendingRepair=" + 
            hasPendingRepair + ", hasActiveReservation=" + hasActiveReservation);
        
        Integer status = calculateStatus(
            availableQuantity,
            borrowed,
            faulty,
            hasPendingRepair,
            hasActiveReservation
        );
        
        equipment.setAvailableQuantity(availableQuantity);
        equipment.setBorrowQuantity(borrowed);
        equipment.setRepairQuantity(faulty);
        equipment.setEquipmentStatus(status);
        
        return updateById(equipment);
    }

    private Integer calculateStatus(Integer availableQuantity, Integer borrowQuantity, Integer repairQuantity, Boolean hasPendingRepair, Boolean hasActiveReservation) {
        if (repairQuantity > 0) {
            return 4;
        }
        if (borrowQuantity > 0 && availableQuantity == 0) {
            return 3;
        }
        if (hasActiveReservation) {
            return 2;
        }
        if (availableQuantity > 0) {
            return 1;
        }
        return 1;
    }

    @Override
    @Transactional
    public boolean borrowEquipment(EquipmentBorrow borrow) {
        boolean success = borrowMapper.insert(borrow) > 0;
        if (!success) {
            return false;
        }
        return updateEquipmentStatus(borrow.getEquipmentId());
    }

    @Override
    @Transactional
    public boolean returnEquipment(Long borrowId) {
        EquipmentBorrow borrow = borrowMapper.selectById(borrowId);
        if (borrow == null) {
            return false;
        }
        
        borrow.setBorrowStatus(2);
        boolean success = borrowMapper.updateById(borrow) > 0;
        if (!success) {
            return false;
        }
        
        return updateEquipmentStatus(borrow.getEquipmentId());
    }

    @Override
    @Transactional
    public boolean approveRepair(Long repairId, String comment, Integer status) {
        EquipmentRepair repair = repairMapper.selectById(repairId);
        if (repair == null) {
            return false;
        }
        
        repair.setRepairStatus(status);
        repair.setAuditResult(comment);
        boolean success = repairMapper.updateById(repair) > 0;
        if (!success) {
            return false;
        }
        
        if (status == 1) {
            return updateEquipmentStatus(repair.getEquipmentId());
        }
        
        return true;
    }
}
