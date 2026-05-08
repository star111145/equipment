package com.swpu.equipment.lifecycle.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swpu.equipment.equipment.entity.Equipment;
import com.swpu.equipment.equipment.mapper.EquipmentMapper;
import com.swpu.equipment.equipment.service.EquipmentService;
import com.swpu.equipment.lifecycle.entity.EquipmentBorrow;
import com.swpu.equipment.lifecycle.entity.EquipmentRepair;
import com.swpu.equipment.lifecycle.entity.EquipmentReturn;
import com.swpu.equipment.lifecycle.mapper.EquipmentBorrowMapper;
import com.swpu.equipment.lifecycle.mapper.EquipmentRepairMapper;
import com.swpu.equipment.lifecycle.mapper.EquipmentReturnMapper;
import com.swpu.equipment.lifecycle.service.EquipmentRepairService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EquipmentRepairServiceImpl extends ServiceImpl<EquipmentRepairMapper, EquipmentRepair> implements EquipmentRepairService {
    
    private static final Logger log = LoggerFactory.getLogger(EquipmentRepairServiceImpl.class);

    @Autowired
    private EquipmentMapper equipmentMapper;
    
    @Autowired
    private EquipmentService equipmentService;
    
    @Autowired
    private EquipmentBorrowMapper borrowMapper;
    
    @Autowired
    private EquipmentReturnMapper returnMapper;

    @Override
    public IPage<EquipmentRepair> getPageListWithType(Page<EquipmentRepair> page, Long userId, Integer repairStatus, Integer auditStatus, String keyword) {
        return baseMapper.getPageListWithType(page, userId, repairStatus, auditStatus, keyword);
    }

    @Transactional
    @Override
    public boolean approveRepair(Long id, String comment, Integer status, Long auditUserId, String auditUserName) {
        EquipmentRepair repair = baseMapper.selectById(id);
        if (repair == null) {
            log.info("=== Debug: 报修记录不存在 id={}", id);
            return false;
        }
        
        log.info("=== Debug: 报修记录 id={}, equipmentId={}, userId={}, repairQuantity={}", 
            repair.getId(), repair.getEquipmentId(), repair.getUserId(), repair.getRepairQuantity());
        
        repair.setAuditStatus(status);
        repair.setAuditResult(comment);
        repair.setAuditUserId(auditUserId);
        repair.setAuditUserName(auditUserName);
        repair.setAuditTime(LocalDateTime.now());
        
        if (status == 1) { // 同意报修
            repair.setRepairStatus(1); // 报修中
            
            Long equipmentId = repair.getEquipmentId();
            Long userId = repair.getUserId();
            log.info("=== Debug: 查询未归还借用, equipmentId={}, userId={}", equipmentId, userId);
            
            List<EquipmentBorrow> unreturnedBorrows = borrowMapper.findUnreturnedBorrowsByEquipmentAndUser(
                equipmentId, userId
            );
            
            log.info("=== Debug: 找到未归还借用记录数 = {}", (unreturnedBorrows != null ? unreturnedBorrows.size() : 0));
            log.info("=== Debug: 报修数量 = {}", repair.getRepairQuantity());
            
            if (unreturnedBorrows != null && !unreturnedBorrows.isEmpty()) {
                EquipmentBorrow borrow = unreturnedBorrows.get(0);
                
                log.info("=== Debug: 创建归还记录, borrowId={}, 归还数量={}", borrow.getId(), repair.getRepairQuantity());
                
                EquipmentReturn equipmentReturn = new EquipmentReturn();
                equipmentReturn.setBorrowId(borrow.getId());
                equipmentReturn.setEquipmentId(borrow.getEquipmentId());
                equipmentReturn.setEquipmentNumber(borrow.getEquipmentNumber());
                equipmentReturn.setEquipmentName(borrow.getEquipmentName());
                equipmentReturn.setEquipmentModel(borrow.getEquipmentModel());
                equipmentReturn.setEquipmentImage(borrow.getEquipmentImage());
                equipmentReturn.setEquipmentTypeId(borrow.getEquipmentTypeId());
                equipmentReturn.setUserId(borrow.getUserId());
                equipmentReturn.setRealName(borrow.getRealName());
                equipmentReturn.setReturnQuantity(repair.getRepairQuantity());
                equipmentReturn.setRepairQuantity(repair.getRepairQuantity());
                equipmentReturn.setOriginalBorrowQuantity(borrow.getBorrowQuantity() + repair.getRepairQuantity());
                equipmentReturn.setExpectedReturnTime(borrow.getExpectedReturnTime());
                equipmentReturn.setActualReturnTime(LocalDateTime.now());
                equipmentReturn.setReturnTime(LocalDateTime.now());
                equipmentReturn.setReturnStatus(1);
                equipmentReturn.setAuditStatus(1);
                equipmentReturn.setAuditResult("报修通过自动归还");
                equipmentReturn.setPurpose(borrow.getPurpose());
                equipmentReturn.setAuditTime(LocalDateTime.now());
                equipmentReturn.setAuditUserId(auditUserId);
                equipmentReturn.setAuditUserName(auditUserName);
                returnMapper.insert(equipmentReturn);
                
                int remainingQuantity = borrow.getBorrowQuantity() - repair.getRepairQuantity();
                System.out.println("【报修通过】borrowId=" + borrow.getId() + ", 原借用数=" + borrow.getBorrowQuantity() + ", 报修数=" + repair.getRepairQuantity() + ", 剩余=" + remainingQuantity);
                if (remainingQuantity <= 0) {
                    borrow.setBorrowStatus(2);
                    borrow.setBorrowQuantity(0);
                } else {
                    borrow.setBorrowQuantity(remainingQuantity);
                }
                borrowMapper.updateById(borrow);
                System.out.println("【报修通过】借用记录已更新, borrowId=" + borrow.getId() + ", 新状态=" + borrow.getBorrowStatus() + ", 新数量=" + borrow.getBorrowQuantity());
            }
             
        } else if (status == 2) { // 拒绝报修
            repair.setRepairStatus(3); // 已拒绝
        }
        
        baseMapper.updateById(repair);
        
        log.info("【报修通过】调用 updateEquipmentStatus, equipmentId={}", repair.getEquipmentId());
        equipmentService.updateEquipmentStatus(repair.getEquipmentId());
        
        return true;
    }

    @Transactional
    @Override
    public boolean completeRepair(Long id, String comment) {
        EquipmentRepair repair = baseMapper.selectById(id);
        if (repair == null) {
            log.info("=== Debug: 维修记录不存在 id={}", id);
            return false;
        }
        
        log.info("=== Debug: 完成维修 id={}, equipmentId={}, repairQuantity={}", 
            repair.getId(), repair.getEquipmentId(), repair.getRepairQuantity());
        
        repair.setRepairStatus(2); // 已维修
        baseMapper.updateById(repair);
        
        log.info("=== Debug: 调用 updateEquipmentStatus");
        equipmentService.updateEquipmentStatus(repair.getEquipmentId());
        
        return true;
    }
    
    @Transactional
    @Override
    public boolean cancelRepair(Long id) {
        EquipmentRepair repair = baseMapper.selectById(id);
        if (repair == null) {
            return false;
        }
        
        boolean wasApproved = repair.getAuditStatus() == 1 && repair.getRepairStatus() == 1;
        
        repair.setRepairStatus(4); // 已取消
        repair.setAuditStatus(1);
        repair.setAuditResult("用户取消报修");
        baseMapper.updateById(repair);
        
        if (wasApproved) {
            Equipment equipment = equipmentMapper.selectById(repair.getEquipmentId());
            Long userBorrowCount = borrowMapper.countUserBorrowedEquipment(
                repair.getEquipmentId(), 
                repair.getUserId()
            );
            
            if (userBorrowCount == 0) {
                Integer newAvailable = equipment.getAvailableQuantity() + repair.getRepairQuantity();
                equipment.setAvailableQuantity(Math.min(equipment.getStockQuantity(), newAvailable));
                equipmentMapper.updateById(equipment);
            }
        }
        
        equipmentService.updateEquipmentStatus(repair.getEquipmentId());
        
        return true;
    }
    
    @Override
    public Long getUserPendingRepairQuantity(Long equipmentId, Long userId) {
        return baseMapper.getUserPendingRepairQuantity(equipmentId, userId);
    }
}
