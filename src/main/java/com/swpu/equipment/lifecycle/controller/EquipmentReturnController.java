package com.swpu.equipment.lifecycle.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.swpu.equipment.common.util.Result;
import com.swpu.equipment.common.util.TokenUtil;
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
@RequestMapping("/lifecycle/return")
public class EquipmentReturnController {

    @Autowired
    private EquipmentReturnService equipmentReturnService;

    @Autowired
    private EquipmentBorrowService equipmentBorrowService;

    @Autowired
    private UserService userService;

    @Autowired
    private TokenUtil tokenUtil;

    @GetMapping("/list")
    public Result<IPage<EquipmentReturn>> getReturnList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) String keyword) {
        Page<EquipmentReturn> page = new Page<>(current, size);
        IPage<EquipmentReturn> result = equipmentReturnService.getPageListWithType(page, null, status, auditStatus, keyword);
        
        return Result.success(result);
    }

    @GetMapping("/user/list")
    public Result<IPage<EquipmentReturn>> getUserReturnList(
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

        Page<EquipmentReturn> page = new Page<>(current, size);
        IPage<EquipmentReturn> result = equipmentReturnService.getPageListWithType(page, userId, status, null, keyword);
        
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<EquipmentReturn> getReturnById(@PathVariable Long id) {
        EquipmentReturn returnRecord = equipmentReturnService.getById(id);
        if (returnRecord == null) {
            return Result.error("记录不存在");
        }
        return Result.success(returnRecord);
    }

    @GetMapping("/user/{id}")
    public Result<EquipmentReturn> getUserReturnById(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("未登录");
        }
        
        EquipmentReturn returnRecord = equipmentReturnService.getById(id);
        if (returnRecord == null) {
            return Result.error("记录不存在");
        }
        if (!returnRecord.getUserId().equals(userId)) {
            return Result.error("无权限查看此记录");
        }
        return Result.success(returnRecord);
    }



    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Object> auditReturn(@PathVariable Long id, @RequestBody EquipmentReturn auditInfo, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long auditUserId = tokenUtil.getUserIdFromToken(token);
        if (auditUserId == null) {
            return Result.error("未登录");
        }

        User auditUser = userService.getById(auditUserId);
        if (auditUser == null) {
            return Result.error("审核人不存在");
        }

        EquipmentReturn returnRecord = equipmentReturnService.getById(id);
        if (returnRecord == null) {
            return Result.error("归还记录不存在");
        }
        
        if (returnRecord.getReturnStatus() != 0) {
            return Result.error("该归还已被处理，无法重复审核");
        }

        returnRecord.setAuditStatus(auditInfo.getAuditStatus());
        returnRecord.setAuditResult(auditInfo.getAuditResult());
        returnRecord.setAuditUserId(auditUserId);
        returnRecord.setAuditUserName(auditUser.getRealName());
        returnRecord.setAuditTime(LocalDateTime.now());

        if (auditInfo.getAuditStatus() == 1) {
            returnRecord.setReturnStatus(1);
            EquipmentBorrow borrow = equipmentBorrowService.getById(returnRecord.getBorrowId());
            if (borrow != null) {
                returnRecord.setExpectedReturnTime(borrow.getExpectedReturnTime());
            }
            returnRecord.setActualReturnTime(LocalDateTime.now());
            equipmentBorrowService.returnEquipment(returnRecord.getBorrowId(), returnRecord.getReturnQuantity());
        } else if (auditInfo.getAuditStatus() == 2) {
            returnRecord.setReturnStatus(2);
        }

        equipmentReturnService.updateById(returnRecord);

        Map<String, Object> data = new HashMap<>();
        data.put("message", "归还验收成功");
        data.put("needRefresh", true);
        WebSocketHandler.sendToAll("{\"type\":\"return_refresh\"}");
        return Result.success(data);
    }

    /**
     * 取消归还（管理员）
     */
    @PutMapping("/{id}/admin-cancel")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Void> adminCancelReturn(@PathVariable Long id, @RequestBody Map<String, String> params, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long adminUserId = tokenUtil.getUserIdFromToken(token);
        if (adminUserId == null) {
            return Result.error("未登录");
        }

        EquipmentReturn returnRecord = equipmentReturnService.getById(id);
        if (returnRecord == null) {
            return Result.error("归还记录不存在");
        }

        String reason = params.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            return Result.error("请填写取消原因");
        }

        returnRecord.setAuditStatus(2);
        returnRecord.setAuditResult("管理员取消: " + reason);
        returnRecord.setAuditUserId(adminUserId);
        returnRecord.setAuditTime(LocalDateTime.now());

        boolean success = equipmentReturnService.updateById(returnRecord);
        if (success) {
            WebSocketHandler.sendToAll("{\"type\":\"return_refresh\"}");
            return Result.success();
        } else {
            return Result.error("取消归还失败");
        }
    }
}
