package com.swpu.equipment.lifecycle.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.swpu.equipment.common.util.Result;
import com.swpu.equipment.common.util.TokenUtil;
import com.swpu.equipment.equipment.entity.Equipment;
import com.swpu.equipment.equipment.service.EquipmentService;
import com.swpu.equipment.lifecycle.entity.EquipmentRepair;
import com.swpu.equipment.lifecycle.service.EquipmentBorrowService;
import com.swpu.equipment.lifecycle.service.EquipmentRepairService;
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
@RequestMapping("/lifecycle/repair")
public class EquipmentRepairController {

    @Autowired
    private EquipmentRepairService equipmentRepairService;

    @Autowired
    private EquipmentBorrowService equipmentBorrowService;

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private UserService userService;

    @Autowired
    private TokenUtil tokenUtil;

    /**
     * 提交报修申请
     */
    @PostMapping
    public Result<Object> submitRepair(@RequestBody EquipmentRepair repair, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        String role = tokenUtil.getRoleFromToken(token);
        if (userId == null) {
            return Result.error("未登录");
        }

        Equipment equipment = equipmentService.getById(repair.getEquipmentId());
        if (equipment == null) {
            return Result.error("设备不存在");
        }

        equipmentService.updateEquipmentStatus(repair.getEquipmentId());
        equipment = equipmentService.getById(repair.getEquipmentId());

        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        if (equipmentBorrowService.hasPendingApplication(userId, repair.getEquipmentId())) {
            return Result.error("您有待审核的申请，请等待审核完成后再提交");
        }
        
        boolean isAdmin = "admin".equals(role);
        
        System.out.println("========================================");
        System.out.println("【报修验证】isAdmin=" + isAdmin + ", role=" + role);
        System.out.println("========================================");
        
        if (repair.getRepairQuantity() == null || repair.getRepairQuantity() <= 0) {
            return Result.error("报修数量必须大于0");
        }
        
        if (isAdmin) {
            Integer stockQuantity = equipment.getStockQuantity();
            if (stockQuantity == null) {
                stockQuantity = 0;
            }
            System.out.println("【报修验证-管理员】库存数量=" + stockQuantity + ", 报修数量=" + repair.getRepairQuantity());
            if (repair.getRepairQuantity() > stockQuantity) {
                return Result.error("报修数量不能超过库存数量");
            }
        } else {
            Long userBorrowQuantity = equipmentBorrowService.getUserUnreturnedBorrowQuantity(repair.getEquipmentId(), userId);
            if (userBorrowQuantity == null) {
                userBorrowQuantity = 0L;
            }
            Long userPendingRepairQuantity = equipmentRepairService.getUserPendingRepairQuantity(repair.getEquipmentId(), userId);
            System.out.println("【报修验证-用户】用户Id=" + userId + ", 设备Id=" + repair.getEquipmentId() + ", 未归还=" + userBorrowQuantity + ", 待审核报修=" + userPendingRepairQuantity + ", 申请报修=" + repair.getRepairQuantity());
            if (userPendingRepairQuantity != null && userPendingRepairQuantity > 0) {
                return Result.error("您有待处理的报修申请，请等待审核完成后再提交新的报修");
            }
            if (repair.getRepairQuantity() > userBorrowQuantity) {
                return Result.error("报修数量不能超过您的借出数量");
            }
        }

        repair.setUserId(userId);
        repair.setRealName(user.getRealName());
        repair.setPhone(user.getPhone());
        repair.setEquipmentNumber(equipment.getEquipmentNumber());
        repair.setEquipmentName(equipment.getEquipmentName());
        repair.setEquipmentModel(equipment.getEquipmentModel());
        repair.setEquipmentImage(equipment.getEquipmentImage());
        repair.setEquipmentTypeId(equipment.getEquipmentTypeId());
        repair.setEquipmentTypeName(equipment.getEquipmentType());
        repair.setFaultDescription(repair.getFaultDescription());
        repair.setFaultImageList(repair.getFaultImageList());
        repair.setRepairTime(LocalDateTime.now());
        repair.setRepairStatus(0);
        repair.setAuditStatus(0);

        boolean success = equipmentRepairService.save(repair);
        if (!success) {
            return Result.error("报修申请提交失败");
        }

        equipmentService.updateEquipmentStatus(repair.getEquipmentId());

        Map<String, Object> data = new HashMap<>();
        data.put("message", "报修申请已提交，等待管理员审核");
        return Result.success("报修申请已提交，等待管理员审核");
    }

    /**
     * 获取我的报修列表（分页）
     */
    @GetMapping("/user/list")
    public Result<IPage<EquipmentRepair>> getUserRepairList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("未登录");
        }

        Page<EquipmentRepair> page = new Page<>(current, size);
        IPage<EquipmentRepair> result = equipmentRepairService.getPageListWithType(page, userId, status, null, null);
        
        return Result.success(result);
    }

    /**
     * 获取报修列表（分页）
     */
    @GetMapping("/list")
    public Result<IPage<EquipmentRepair>> getAdminRepairList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("未登录");
        }

        User user = userService.getById(userId);
        boolean isAdmin = user != null && user.isAdmin();

        Page<EquipmentRepair> page = new Page<>(current, size);
        IPage<EquipmentRepair> result = equipmentRepairService.getPageListWithType(page, isAdmin ? null : userId, status, auditStatus, keyword);
        
        return Result.success(result);
    }

    /**
     * 获取报修详情
     */
    @GetMapping("/{id}")
    public Result<EquipmentRepair> getRepairDetail(@PathVariable Long id) {
        EquipmentRepair repair = equipmentRepairService.getById(id);
        if (repair == null) {
            return Result.error("报修记录不存在");
        }
        if (repair.getEquipmentId() != null) {
            Equipment equipment = equipmentService.getById(repair.getEquipmentId());
            if (equipment != null) {
                repair.setEquipmentTypeName(equipment.getEquipmentType());
            }
        }
        return Result.success(repair);
    }

    @GetMapping("/user/{id}")
    public Result<EquipmentRepair> getUserRepairById(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("未登录");
        }
        
        EquipmentRepair repair = equipmentRepairService.getById(id);
        if (repair == null) {
            return Result.error("报修记录不存在");
        }
        if (!repair.getUserId().equals(userId)) {
            return Result.error("无权限查看此记录");
        }
        return Result.success(repair);
    }

    /**
     * 审核报修申请（管理员）
     */
    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Object> auditRepair(@PathVariable Long id, @RequestBody EquipmentRepair auditInfo, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long auditUserId = tokenUtil.getUserIdFromToken(token);
        if (auditUserId == null) {
            return Result.error("未登录");
        }

        User auditUser = userService.getById(auditUserId);
        if (auditUser == null) {
            return Result.error("审核人不存在");
        }

        EquipmentRepair repair = equipmentRepairService.getById(id);
        if (repair == null) {
            return Result.error("报修记录不存在");
        }
        
        if (repair.getRepairStatus() != 0) {
            return Result.error("该报修已被处理，无法重复审核");
        }

        boolean success = equipmentRepairService.approveRepair(id, auditInfo.getAuditResult(), auditInfo.getAuditStatus(), auditUserId, auditUser.getRealName());
        if (!success) {
            return Result.error("审核失败");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("message", "报修审核成功");
        data.put("needRefresh", true);
        WebSocketHandler.sendToAll("{\"type\":\"repair_refresh\"}");
        return Result.success(data);
    }

    /**
     * 更新维修状态（管理员）
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Object> updateRepairStatus(@PathVariable Long id, @RequestParam Integer status, 
            @RequestParam(required = false) String reason, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long auditUserId = tokenUtil.getUserIdFromToken(token);
        if (auditUserId == null) {
            return Result.error("未登录");
        }

        User auditUser = userService.getById(auditUserId);
        if (auditUser == null) {
            return Result.error("审核人不存在");
        }

        if ((status == 3 || status == 4) && (reason == null || reason.trim().isEmpty())) {
            return Result.error("请输入原因");
        }

        EquipmentRepair repair = equipmentRepairService.getById(id);
        if (repair == null) {
            return Result.error("报修记录不存在");
        }

        repair.setRepairStatus(status);
        repair.setAuditUserId(auditUserId);
        repair.setAuditUserName(auditUser.getRealName());
        repair.setAuditTime(LocalDateTime.now());
        
        if (status == 3) {
            repair.setAuditResult("已拒绝：" + reason);
        } else if (status == 4) {
            repair.setAuditResult("已取消：" + reason);
        }

        equipmentRepairService.updateById(repair);
        
        if (status == 2 || status == 3 || status == 4) {
            equipmentService.updateEquipmentStatus(repair.getEquipmentId());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("message", "维修状态更新成功");
        data.put("needRefresh", true);
        WebSocketHandler.sendToAll("{\"type\":\"repair_refresh\"}");
        return Result.success(data);
    }

    /**
     * 删除报修记录
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteRepair(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("未登录");
        }

        EquipmentRepair repair = equipmentRepairService.getById(id);
        if (repair == null) {
            return Result.error("报修记录不存在");
        }

        User user = userService.getById(userId);
        
        // 只有管理员可以删除任何记录，用户只能删除自己的待审核记录
        if (user.isAdmin()) {
            // 管理员可以删除任何记录
        } else if (!repair.getUserId().equals(userId)) {
            return Result.error("无权删除");
        } else if (repair.getAuditStatus() != 0) {
            return Result.error("已审核的报修记录不能删除");
        }

        equipmentRepairService.removeById(id);
        equipmentService.updateEquipmentStatus(repair.getEquipmentId());

        return Result.success();
    }

    /**
     * 取消报修申请（用户）
     */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancelRepair(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("未登录");
        }

        EquipmentRepair repair = equipmentRepairService.getById(id);
        if (repair == null) {
            return Result.error("报修记录不存在");
        }

        if (!repair.getUserId().equals(userId)) {
            return Result.error("无权操作此报修记录");
        }

        if (repair.getAuditStatus() != 0) {
            return Result.error("已审核的报修记录不能取消");
        }

        boolean success = equipmentRepairService.cancelRepair(id);
        if (!success) {
            return Result.error("取消报修失败");
        }

        return Result.success();
    }

    /**
     * 获取我的报修列表
     */
    @GetMapping("/my")
    public Result<List<EquipmentRepair>> getMyRepairs(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("未登录");
        }

        LambdaQueryWrapper<EquipmentRepair> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EquipmentRepair::getUserId, userId)
                .orderByDesc(EquipmentRepair::getCreateTime);

        List<EquipmentRepair> list = equipmentRepairService.list(wrapper);
        return Result.success(list);
    }
    
    @GetMapping("/export")
    public void exportRepair(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(defaultValue = "false") Boolean exportAll,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletResponse response) throws IOException {
        if (exportAll == null) {
            exportAll = false;
        }
        int querySize = exportAll ? 100000 : size;
        Page<EquipmentRepair> page = new Page<>(current, querySize);
        IPage<EquipmentRepair> result = equipmentRepairService.getPageListWithType(page, null, status, auditStatus, keyword);
        List<EquipmentRepair> list = result.getRecords();
        
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = "报修记录_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        
        EasyExcel.write(response.getOutputStream(), RepairExcelData.class)
                .sheet("报修记录")
                .doWrite(list.stream().map(this::convertToExcelData).collect(Collectors.toList()));
    }
    
    private RepairExcelData convertToExcelData(EquipmentRepair repair) {
        RepairExcelData data = new RepairExcelData();
        data.setId(repair.getId() != null ? repair.getId().intValue() : null);
        data.setEquipmentName(repair.getEquipmentName());
        data.setEquipmentNumber(repair.getEquipmentNumber());
        data.setEquipmentModel(repair.getEquipmentModel());
        data.setUserName(repair.getRealName());
        data.setPhone(repair.getPhone());
        data.setRepairQuantity(repair.getRepairQuantity());
        data.setFaultDescription(repair.getFaultDescription());
        data.setRepairStatus(getRepairStatusText(repair.getRepairStatus()));
        data.setAuditStatus(getAuditStatusText(repair.getAuditStatus()));
        data.setAuditUserName(repair.getAuditUserName());
        data.setAuditTime(repair.getAuditTime() != null ? repair.getAuditTime().toString() : "");
        data.setAuditResult(repair.getAuditResult());
        data.setCreateTime(repair.getCreateTime() != null ? repair.getCreateTime().toString() : "");
        return data;
    }
    
    private String getRepairStatusText(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "待审核";
            case 1: return "维修中";
            case 2: return "已维修";
            case 3: return "已拒绝";
            case 4: return "已取消";
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

class RepairExcelData {
    private Integer id;
    private String equipmentName;
    private String equipmentNumber;
    private String equipmentModel;
    private String userName;
    private String phone;
    private Integer repairQuantity;
    private String faultDescription;
    private String repairStatus;
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
    public Integer getRepairQuantity() { return repairQuantity; }
    public void setRepairQuantity(Integer repairQuantity) { this.repairQuantity = repairQuantity; }
    public String getFaultDescription() { return faultDescription; }
    public void setFaultDescription(String faultDescription) { this.faultDescription = faultDescription; }
    public String getRepairStatus() { return repairStatus; }
    public void setRepairStatus(String repairStatus) { this.repairStatus = repairStatus; }
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
