package com.swpu.equipment.lifecycle.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swpu.equipment.common.util.Result;
import com.swpu.equipment.common.util.TokenUtil;
import com.swpu.equipment.equipment.entity.Equipment;
import com.swpu.equipment.equipment.service.EquipmentService;
import com.swpu.equipment.lifecycle.entity.EquipmentBorrow;
import com.swpu.equipment.lifecycle.entity.EquipmentReturn;
import com.swpu.equipment.lifecycle.service.EquipmentBorrowService;
import com.swpu.equipment.lifecycle.service.EquipmentReturnService;
import com.swpu.equipment.user.entity.User;
import com.swpu.equipment.user.service.UserService;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.swpu.equipment.common.websocket.WebSocketHandler;

@RestController
@RequestMapping("/lifecycle/borrow")
public class EquipmentBorrowController {

    @Autowired
    private EquipmentBorrowService equipmentBorrowService;

    @Autowired
    private EquipmentReturnService equipmentReturnService;

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private UserService userService;

    @Autowired
    private TokenUtil tokenUtil;

    /**
     * 获取借用列表（管理员）
     */
    @GetMapping("/list")
    public Result<IPage<EquipmentBorrow>> getBorrowList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) String keyword) {
        Page<EquipmentBorrow> page = new Page<>(current, size);
        return Result.success(equipmentBorrowService.getPageListWithType(page, null, status, auditStatus, keyword));
    }

    /**
     * 获取用户的借用列表（普通用户）
     */
    @GetMapping("/user/list")
    public Result<IPage<EquipmentBorrow>> getUserBorrowList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("未登录");
        }

        Page<EquipmentBorrow> page = new Page<>(current, size);
        return Result.success(equipmentBorrowService.getPageListWithType(page, userId, status, null, keyword));
    }

    @GetMapping("/{id}")
    public Result<EquipmentBorrow> getBorrowById(@PathVariable Long id) {
        EquipmentBorrow borrow = equipmentBorrowService.getById(id);
        if (borrow == null) {
            return Result.error("记录不存在");
        }
        return Result.success(borrow);
    }

    @GetMapping("/user/{id}")
    public Result<EquipmentBorrow> getUserBorrowById(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("未登录");
        }
        
        EquipmentBorrow borrow = equipmentBorrowService.getById(id);
        if (borrow == null) {
            return Result.error("记录不存在");
        }
        if (!borrow.getUserId().equals(userId)) {
            return Result.error("无权限查看此记录");
        }
        return Result.success(borrow);
    }

    /**
     * 创建借用申请（用户）
     */
    @PostMapping
    public Result<Object> createBorrow(@RequestBody EquipmentBorrow borrow, HttpServletRequest request) {
        System.out.println("=== Debug: borrowQuantity = " + borrow.getBorrowQuantity());
        
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("未登录");
        }

        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        borrow.setUserId(userId);
        borrow.setRealName(user.getRealName());
        borrow.setPhone(user.getPhone());
        borrow.setBorrowTime(LocalDateTime.now());
        borrow.setBorrowStatus(0);
        borrow.setAuditStatus(0);

        if (borrow.getBorrowQuantity() == null || borrow.getBorrowQuantity() <= 0) {
            return Result.error("请填写有效的借用数量");
        }
        
        Equipment equipment = equipmentService.getById(borrow.getEquipmentId());
        if (equipment == null) {
            return Result.error("设备不存在");
        }
        
        if (equipment.getAvailableQuantity() == null) {
            return Result.error("设备可用数量未知");
        }

        if (borrow.getBorrowQuantity() > equipment.getAvailableQuantity()) {
            return Result.error("可用数量不足");
        }
        
        if (equipmentBorrowService.hasOverdueBorrow(userId)) {
            return Result.error("您有逾期的借用记录，请先完成归还后再借用");
        }
        
        if (equipmentBorrowService.hasUnreturnedBorrow(userId, borrow.getEquipmentId())) {
            return Result.error("您有未归还的该设备，请先完成归还后再借用");
        }
        
        if (equipmentBorrowService.hasPendingApplication(userId, borrow.getEquipmentId())) {
            return Result.error("您有待审核的申请，请等待审核完成后再提交");
        }
        
        borrow.setEquipmentNumber(equipment.getEquipmentNumber());
        borrow.setEquipmentName(equipment.getEquipmentName());
        borrow.setEquipmentModel(equipment.getEquipmentModel());
        borrow.setEquipmentImage(equipment.getEquipmentImage());
        borrow.setEquipmentTypeId(equipment.getEquipmentTypeId());
        borrow.setEquipmentType(equipment.getEquipmentType());

        boolean success = equipmentBorrowService.save(borrow);
        if (!success) {
            return Result.error("借用申请提交失败");
        }

        equipmentService.updateEquipmentStatus(borrow.getEquipmentId());

        Map<String, Object> data = new HashMap<>();
        data.put("message", "借用申请已提交，等待管理员审核");
        return Result.success("借用申请已提交，等待管理员审核");
    }

    /**
     * 归还设备（用户）
     */
    @PutMapping("/{id}/return")
    public Result<Object> returnEquipment(@PathVariable Long id, 
            @RequestParam(required = false) Integer returnQuantity,
            HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("未登录");
        }

        EquipmentBorrow borrow = equipmentBorrowService.getById(id);
        if (borrow == null) {
            return Result.error("借用记录不存在");
        }

        if (!borrow.getUserId().equals(userId)) {
            return Result.error("无权操作此借用记录");
        }

        if (borrow.getBorrowStatus() != 1) {
            return Result.error("只有已借出的记录才能归还");
        }
        
        if (equipmentBorrowService.hasPendingApplication(borrow.getUserId(), borrow.getEquipmentId())) {
            return Result.error("您有待审核的申请，请等待审核完成后再提交");
        }
        
        Equipment equipment = equipmentService.getById(borrow.getEquipmentId());
        if (equipment == null) {
            return Result.error("设备不存在");
        }
        
        Integer actualReturnQuantity = returnQuantity != null ? returnQuantity : borrow.getBorrowQuantity();
        
        if (actualReturnQuantity == null || actualReturnQuantity <= 0) {
            return Result.error("请填写有效的归还数量");
        }
        
        if (actualReturnQuantity > borrow.getBorrowQuantity()) {
            return Result.error("归还数量不能超过借用数量");
        }
        
        EquipmentReturn returnRecord = new EquipmentReturn();
        returnRecord.setBorrowId(borrow.getId());
        returnRecord.setEquipmentId(borrow.getEquipmentId());
        returnRecord.setEquipmentNumber(borrow.getEquipmentNumber());
        returnRecord.setEquipmentName(borrow.getEquipmentName());
        returnRecord.setEquipmentModel(borrow.getEquipmentModel());
        returnRecord.setEquipmentImage(borrow.getEquipmentImage());
        returnRecord.setEquipmentTypeId(borrow.getEquipmentTypeId());
        returnRecord.setUserId(userId);
        returnRecord.setRealName(borrow.getRealName());
        returnRecord.setPurpose(borrow.getPurpose());
        returnRecord.setReturnQuantity(actualReturnQuantity);
        returnRecord.setReturnTime(LocalDateTime.now());
        returnRecord.setReturnStatus(0);
        returnRecord.setAuditStatus(0);
        returnRecord.setAuditResult("待管理员验收");
        returnRecord.setRepairQuantity(0);
        
        boolean returnSuccess = equipmentReturnService.save(returnRecord);
        if (!returnSuccess) {
            return Result.error("归还申请提交失败，请联系管理员");
        }

        return Result.success("归还申请已提交，请等待管理员验收");
    }

    /**
     * 取消借用申请（用户）
     */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancelBorrow(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("未登录");
        }

        EquipmentBorrow borrow = equipmentBorrowService.getById(id);
        if (borrow == null) {
            return Result.error("借用记录不存在");
        }

        if (!borrow.getUserId().equals(userId)) {
            return Result.error("无权操作此借用记录");
        }

        if (borrow.getBorrowStatus() != 0) {
            return Result.error("只有待审核的借用申请才能取消");
        }

        borrow.setBorrowStatus(3);
        borrow.setAuditStatus(1);
        borrow.setAuditResult("用户自行取消");
        borrow.setAuditTime(LocalDateTime.now());
        
        boolean success = equipmentBorrowService.updateById(borrow);
        if (success) {
            return Result.success();
        } else {
            return Result.error("取消借用申请失败");
        }
    }

    /**
     * 审核借用申请（管理员）
     */
    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Object> auditBorrow(@PathVariable Long id, @RequestBody EquipmentBorrow auditInfo, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long auditUserId = tokenUtil.getUserIdFromToken(token);
        if (auditUserId == null) {
            return Result.error("未登录");
        }

        User auditUser = userService.getById(auditUserId);
        if (auditUser == null) {
            return Result.error("审核人不存在");
        }

        EquipmentBorrow borrow = equipmentBorrowService.getById(id);
        if (borrow == null) {
            return Result.error("借用记录不存在");
        }
        
        if (borrow.getBorrowStatus() != 0) {
            return Result.error("该借用已被处理，无法重复审核");
        }

        borrow.setAuditUserId(auditUserId);
        borrow.setAuditUserName(auditUser.getRealName());
        borrow.setAuditTime(LocalDateTime.now());

        equipmentBorrowService.approveBorrow(borrow, auditInfo.getAuditResult(), auditInfo.getAuditStatus());

        Map<String, Object> data = new HashMap<>();
        data.put("message", "借用审核成功");
        data.put("needRefresh", true);
        WebSocketHandler.sendToAll("{\"type\":\"borrow_refresh\"}");
        return Result.success(data);
    }

    /**
     * 取消借用（管理员）
     */
    @PutMapping("/{id}/admin-cancel")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Void> adminCancelBorrow(@PathVariable Long id, @RequestBody Map<String, String> params, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long adminUserId = tokenUtil.getUserIdFromToken(token);
        if (adminUserId == null) {
            return Result.error("未登录");
        }

        EquipmentBorrow borrow = equipmentBorrowService.getById(id);
        if (borrow == null) {
            return Result.error("借用记录不存在");
        }

        if (borrow.getBorrowStatus() != 1) {
            return Result.error("只有已借出的借用记录才能取消");
        }

        String reason = params.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            return Result.error("请填写取消原因");
        }

        borrow.setBorrowStatus(3);
        borrow.setAuditStatus(1);
        borrow.setAuditResult("管理员取消: " + reason);
        borrow.setAuditUserId(adminUserId);
        borrow.setAuditTime(LocalDateTime.now());

        boolean success = equipmentBorrowService.updateById(borrow);
        if (success) {
            WebSocketHandler.sendToAll("{\"type\":\"borrow_refresh\"}");
            return Result.success();
        } else {
            return Result.error("取消借用失败");
        }
    }
}