package com.swpu.equipment.lifecycle.controller;

import com.alibaba.excel.EasyExcel;
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
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    
    @GetMapping("/export")
    public void exportReturn(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "false") Boolean exportAll,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletResponse response) throws IOException {
        if (exportAll == null) {
            exportAll = false;
        }
        int querySize = exportAll ? 100000 : size;
        Page<EquipmentReturn> page = new Page<>(current, querySize);
        IPage<EquipmentReturn> result = equipmentReturnService.getPageListWithType(page, null, status, null, keyword);
        List<EquipmentReturn> list = result.getRecords();
        
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = "归还记录_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        
        EasyExcel.write(response.getOutputStream(), ReturnExcelData.class)
                .sheet("归还记录")
                .doWrite(list.stream().map(this::convertToExcelData).collect(Collectors.toList()));
    }
    
    private ReturnExcelData convertToExcelData(EquipmentReturn returnRecord) {
        ReturnExcelData data = new ReturnExcelData();
        data.setId(returnRecord.getId() != null ? returnRecord.getId().intValue() : null);
        data.setEquipmentName(returnRecord.getEquipmentName());
        data.setEquipmentNumber(returnRecord.getEquipmentNumber());
        data.setEquipmentModel(returnRecord.getEquipmentModel());
        data.setUserName(returnRecord.getRealName());
        data.setPhone(returnRecord.getPhone());
        data.setReturnQuantity(returnRecord.getReturnQuantity());
        data.setRepairQuantity(returnRecord.getRepairQuantity());
        data.setReturnTime(returnRecord.getReturnTime() != null ? returnRecord.getReturnTime().toString() : "");
        data.setPurpose(returnRecord.getPurpose());
        data.setStatus(getReturnStatusText(returnRecord.getReturnStatus()));
        data.setAuditStatus(getAuditStatusText(returnRecord.getAuditStatus()));
        data.setAuditUserName(returnRecord.getAuditUserName());
        data.setAuditTime(returnRecord.getAuditTime() != null ? returnRecord.getAuditTime().toString() : "");
        data.setAuditResult(returnRecord.getAuditResult());
        data.setCreateTime(returnRecord.getCreateTime() != null ? returnRecord.getCreateTime().toString() : "");
        return data;
    }
    
    private String getReturnStatusText(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "待审核";
            case 1: return "已归还";
            case 2: return "已取消";
            default: return "未知";
        }
    }
    
    private String getAuditStatusText(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "待审核";
            case 1: return "已通过";
            case 2: return "已拒绝";
            default: return "未知";
        }
    }
}

class ReturnExcelData {
    private Integer id;
    private String equipmentName;
    private String equipmentNumber;
    private String equipmentModel;
    private String userName;
    private String phone;
    private Integer returnQuantity;
    private Integer repairQuantity;
    private String returnTime;
    private String purpose;
    private String status;
    private String auditStatus;
    private String auditUserName;
    private String auditTime;
    private String auditResult;
    private String createTime;
    
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }
    public String getEquipmentNumber() { return equipmentNumber; }
    public void setEquipmentNumber(String equipmentNumber) { this.equipmentNumber = equipmentNumber; }
    public String getEquipmentModel() { return equipmentModel; }
    public void setEquipmentModel(String equipmentModel) { this.equipmentModel = equipmentModel; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getReturnQuantity() { return returnQuantity; }
    public void setReturnQuantity(Integer returnQuantity) { this.returnQuantity = returnQuantity; }
    public Integer getRepairQuantity() { return repairQuantity; }
    public void setRepairQuantity(Integer repairQuantity) { this.repairQuantity = repairQuantity; }
    public String getReturnTime() { return returnTime; }
    public void setReturnTime(String returnTime) { this.returnTime = returnTime; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAuditStatus() { return auditStatus; }
    public void setAuditStatus(String auditStatus) { this.auditStatus = auditStatus; }
    public String getAuditUserName() { return auditUserName; }
    public void setAuditUserName(String auditUserName) { this.auditUserName = auditUserName; }
    public String getAuditTime() { return auditTime; }
    public void setAuditTime(String auditTime) { this.auditTime = auditTime; }
    public String getAuditResult() { return auditResult; }
    public void setAuditResult(String auditResult) { this.auditResult = auditResult; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}
