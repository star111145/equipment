package com.swpu.equipment.lifecycle.controller;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        
        if (isAdmin) {
            Integer stockQuantity = equipment.getStockQuantity();
            if (stockQuantity == null) {
                stockQuantity = 0;
            }
            if (repair.getRepairQuantity() > stockQuantity) {
                return Result.error("报修数量不能超过库存数量");
            }
        } else {
            Long userBorrowQuantity = equipmentBorrowService.getUserUnreturnedBorrowQuantity(repair.getEquipmentId(), userId);
            if (userBorrowQuantity == null) {
                userBorrowQuantity = 0L;
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
}
