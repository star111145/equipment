package com.swpu.equipment.lifecycle.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swpu.equipment.common.result.Result;
import com.swpu.equipment.common.util.TokenUtil;
import com.swpu.equipment.equipment.entity.Equipment;
import com.swpu.equipment.equipment.service.EquipmentService;
import com.swpu.equipment.lifecycle.entity.EquipmentRepair;
import com.swpu.equipment.lifecycle.service.EquipmentRepairService;
import com.swpu.equipment.user.entity.User;
import com.swpu.equipment.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/lifecycle/repair")
public class EquipmentRepairController {

    @Autowired
    private EquipmentRepairService equipmentRepairService;

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
    public Result<Void> submitRepair(@RequestBody EquipmentRepair repair, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("未登录");
        }

        // 获取设备信息
        Equipment equipment = equipmentService.getById(repair.getEquipmentId());
        if (equipment == null) {
            return Result.error("设备不存在");
        }

        // 获取用户信息
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 设置报修信息
        repair.setUserId(userId);
        repair.setRealName(user.getRealName());
        repair.setPhone(user.getPhone());
        repair.setEquipmentNumber(equipment.getEquipmentNumber());
        repair.setEquipmentName(equipment.getEquipmentName());
        repair.setEquipmentImage(equipment.getEquipmentImage());
        repair.setEquipmentTypeId(equipment.getEquipmentTypeId());
        repair.setRepairTime(LocalDateTime.now());
        repair.setRepairStatus(0);  // 待审核
        repair.setAuditStatus(0);  // 待审核

        equipmentRepairService.save(repair);

        return Result.success();
    }

    /**
     * 获取报修列表（分页）
     */
    @GetMapping("/list")
    public Result<IPage<EquipmentRepair>> getRepairList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer repairStatus,
            HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("未登录");
        }

        Page<EquipmentRepair> page = new Page<>(current, size);
        LambdaQueryWrapper<EquipmentRepair> wrapper = new LambdaQueryWrapper<>();

        // 普通用户只能看到自己的报修，管理员可以看到所有
        User user = userService.getById(userId);
        if (!user.isAdmin()) {
            wrapper.eq(EquipmentRepair::getUserId, userId);
        }

        if (repairStatus != null) {
            wrapper.eq(EquipmentRepair::getRepairStatus, repairStatus);
        }
        wrapper.orderByDesc(EquipmentRepair::getCreateTime);

        IPage<EquipmentRepair> result = equipmentRepairService.page(page, wrapper);
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
        return Result.success(repair);
    }

    /**
     * 审核报修申请（管理员）
     */
    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Void> auditRepair(@PathVariable Long id, @RequestBody EquipmentRepair auditInfo) {
        EquipmentRepair repair = equipmentRepairService.getById(id);
        if (repair == null) {
            return Result.error("报修记录不存在");
        }

        repair.setAuditStatus(auditInfo.getAuditStatus());
        repair.setAuditResult(auditInfo.getAuditResult());

        // 如果审核通过，设置报修状态为报修中
        if (auditInfo.getAuditStatus() == 1) {
            repair.setRepairStatus(1);
            // 更新设备状态为故障
            Equipment equipment = equipmentService.getById(repair.getEquipmentId());
            if (equipment != null) {
                equipment.setEquipmentStatus(0);  // 故障
                equipmentService.updateById(equipment);
            }
        } else if (auditInfo.getAuditStatus() == 2) {
            // 审核拒绝
            repair.setRepairStatus(4);
        }

        equipmentRepairService.updateById(repair);
        return Result.success();
    }

    /**
     * 更新维修状态（管理员）
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Void> updateRepairStatus(@PathVariable Long id, @RequestParam Integer status) {
        EquipmentRepair repair = equipmentRepairService.getById(id);
        if (repair == null) {
            return Result.error("报修记录不存在");
        }

        repair.setRepairStatus(status);

        // 如果维修完成，更新设备状态为正常
        if (status == 3) {
            Equipment equipment = equipmentService.getById(repair.getEquipmentId());
            if (equipment != null) {
                equipment.setEquipmentStatus(1);  // 正常
                equipmentService.updateById(equipment);
            }
        }

        equipmentRepairService.updateById(repair);
        return Result.success();
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

        // 只能删除自己的报修，或者管理员可以删除所有
        User user = userService.getById(userId);
        if (!user.isAdmin() && !repair.getUserId().equals(userId)) {
            return Result.error("无权删除");
        }

        equipmentRepairService.removeById(id);
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
