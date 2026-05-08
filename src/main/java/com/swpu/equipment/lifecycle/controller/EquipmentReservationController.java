package com.swpu.equipment.lifecycle.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swpu.equipment.common.util.Result;
import com.swpu.equipment.common.util.TokenUtil;
import com.swpu.equipment.common.websocket.WebSocketHandler;
import com.swpu.equipment.lifecycle.entity.EquipmentReservation;
import com.swpu.equipment.lifecycle.entity.EquipmentReservationVO;
import com.swpu.equipment.lifecycle.service.EquipmentReservationService;
import com.swpu.equipment.user.entity.User;
import com.swpu.equipment.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/lifecycle/reserve")
public class EquipmentReservationController {
    
    @Autowired
    private EquipmentReservationService reservationService;
    
    @Autowired
    private TokenUtil tokenUtil;
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/user/list")
    public Result<IPage<EquipmentReservationVO>> getUserReserveList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "-1") Integer status,
            HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("未登录");
        }
        
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        Page<EquipmentReservationVO> page = new Page<>(current, size);
        IPage<EquipmentReservationVO> result = reservationService.getPageListByUserId(page, userId, keyword, status);
        
        return Result.success(result);
    }
    
    @GetMapping("/list")
    public Result<IPage<EquipmentReservationVO>> getAdminReserveList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "-1") Integer status,
            @RequestParam(defaultValue = "-1") Integer auditStatus,
            HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("未登录");
        }
        
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        Page<EquipmentReservationVO> page = new Page<>(current, size);
        IPage<EquipmentReservationVO> result = reservationService.getPageList(page, keyword, status, auditStatus);
        
        return Result.success(result);
    }
    
    @GetMapping("/{id}")
    public Result<EquipmentReservationVO> getReservationById(@PathVariable Long id) {
        EquipmentReservationVO reservation = reservationService.getReservationById(id);
        if (reservation == null) {
            return Result.error("预约不存在");
        }
        return Result.success(reservation);
    }

    @GetMapping("/user/{id}")
    public Result<EquipmentReservationVO> getUserReservationById(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("未登录");
        }
        
        EquipmentReservationVO reservation = reservationService.getReservationById(id);
        if (reservation == null) {
            return Result.error("预约不存在");
        }
        if (!reservation.getUserId().equals(userId)) {
            return Result.error("无权限查看此预约");
        }
        return Result.success(reservation);
    }
    
    @PostMapping
    public Result<Object> createReservation(@RequestBody EquipmentReservation reservation, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("未登录");
        }
        
        reservation.setUserId(userId);
        
        if (reservationService.hasOverdueBorrow(userId)) {
            return Result.error("您有逾期的借用记录，请先完成归还后再预约");
        }
        
        // 先检查待审核预约
        if (reservationService.hasPendingReservation(userId, reservation.getEquipmentId())) {
            return Result.error("您已存在该设备的待审核预约，请等待审核完成后再预约");
        }
        
        // 再检查有效预约（已通过且未过期）
        if (reservationService.hasActiveReservation(userId, reservation.getEquipmentId())) {
            return Result.error("您已存在该设备的有效预约，请完成后再预约");
        }
        
        boolean created = reservationService.createReservation(reservation);
        if (created) {
            return Result.success("预约申请已提交，等待管理员审核");
        } else {
            return Result.error("预约申请提交失败");
        }
    }
    
    @PutMapping("/{id}/cancel")
    public Result<String> cancelReservation(@PathVariable Long id, 
                                           @RequestBody java.util.Map<String, Object> params,
                                           HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        String role = tokenUtil.getRoleFromToken(token);
        
        if (userId == null) {
            return Result.error("未登录");
        }
        
        String cancelReason = params.get("cancelReason") != null ? params.get("cancelReason").toString() : "";
        
        EquipmentReservation reservation = reservationService.getById(id);
        if (reservation == null) {
            return Result.error("预约不存在");
        }
        
        boolean isAdmin = "admin".equals(role);
        
        if (!isAdmin && !reservation.getUserId().equals(userId)) {
            return Result.error("无权操作此预约记录");
        }
        
        if (!isAdmin && reservation.getAuditStatus() != 0) {
            return Result.error("已审核的预约记录不能取消");
        }
        
        if (isAdmin && (cancelReason == null || cancelReason.trim().isEmpty())) {
            return Result.error("管理员取消预约必须填写理由");
        }
        
        reservation.setReserveStatus(3);
        
        if (isAdmin) {
            reservation.setAuditStatus(1);
            reservation.setAuditResult("管理员取消: " + cancelReason);
            reservation.setAuditUserId(userId);
            reservation.setAuditTime(java.time.LocalDateTime.now());
        } else {
            reservation.setAuditStatus(1);
            reservation.setAuditResult("用户取消预约");
        }
        
        boolean updated = reservationService.updateById(reservation);
        
        if (updated) {
            WebSocketHandler.sendToAll("{\"type\":\"reservation_refresh\"}");
            return Result.success("预约取消成功");
        } else {
            return Result.error("预约取消失败");
        }
    }
    
    /**
     * 删除预约记录（用户）
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteReservation(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("未登录");
        }
        
        EquipmentReservation reservation = reservationService.getById(id);
        if (reservation == null) {
            return Result.error("预约不存在");
        }
        
        if (!reservation.getUserId().equals(userId)) {
            return Result.error("无权删除");
        }
        
        // 只能删除待审核的预约
        if (reservation.getAuditStatus() != 0) {
            return Result.error("已审核的预约记录不能删除");
        }
        
        reservationService.removeById(id);
        
        return Result.success();
    }
    
    @PutMapping("/{id}")
    public Result<String> updateReservation(
            @PathVariable Long id,
            @RequestBody EquipmentReservation reservation,
            HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("未登录");
        }
        
        EquipmentReservation existing = reservationService.getById(id);
        if (existing == null) {
            return Result.error("预约记录不存在");
        }
        
        if (!existing.getUserId().equals(userId)) {
            return Result.error("无权限修改此预约");
        }
        
        if (existing.getAuditStatus() != 0) {
            return Result.error("只有待审核的预约可以修改");
        }
        
        reservation.setId(id);
        reservation.setUpdateTime(java.time.LocalDateTime.now());
        boolean updated = reservationService.updateReservation(reservation);
        
        if (updated) {
            return Result.success("预约修改成功");
        } else {
            return Result.error("预约修改失败");
        }
    }
    
    @PostMapping("/extend")
    public Result<String> applyExtend(
            @RequestBody java.util.Map<String, Object> params,
            HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("未登录");
        }
        
        Long originalReservationId = Long.parseLong(params.get("originalReservationId").toString());
        Integer newReserveDuration = Integer.parseInt(params.get("newReserveDuration").toString());
        String extendReason = params.get("extendReason").toString();
        
        EquipmentReservation original = reservationService.getById(originalReservationId);
        if (original == null) {
            return Result.error("原预约记录不存在");
        }
        
        if (!original.getUserId().equals(userId)) {
            return Result.error("无权限操作此预约");
        }
        
        if (original.getReserveStatus() != 1 || original.getAuditStatus() != 1) {
            return Result.error("只有已通过的预约可以申请延期");
        }
        
        LocalDateTime newReserveTime = original.getReserveTime().plusHours(original.getReserveDuration());
        
        boolean hasConflict = reservationService.checkConflict(original.getEquipmentId(), newReserveTime, newReserveDuration, null);
        if (hasConflict) {
            return Result.error("新预约时段存在冲突，请选择其他时间");
        }
        
        EquipmentReservation extendReservation = new EquipmentReservation();
        extendReservation.setEquipmentId(original.getEquipmentId());
        extendReservation.setEquipmentNumber(original.getEquipmentNumber());
        extendReservation.setEquipmentName(original.getEquipmentName());
        extendReservation.setEquipmentModel(original.getEquipmentModel());
        extendReservation.setEquipmentImage(original.getEquipmentImage());
        extendReservation.setEquipmentTypeId(original.getEquipmentTypeId());
        extendReservation.setUserId(userId);
        extendReservation.setReserveTime(newReserveTime);
        extendReservation.setReserveDuration(newReserveDuration);
        extendReservation.setRealName(original.getRealName());
        extendReservation.setPhone(original.getPhone());
        extendReservation.setPurpose("延期申请：" + extendReason);
        extendReservation.setReserveStatus(0);
        extendReservation.setAuditStatus(0);
        extendReservation.setIsExtension(1);
        extendReservation.setCreateTime(LocalDateTime.now());
        extendReservation.setUpdateTime(LocalDateTime.now());
        
        boolean created = reservationService.createReservation(extendReservation);
        if (created) {
            return Result.success("延期申请已提交，等待管理员审核");
        } else {
            return Result.error("延期申请提交失败");
        }
    }
    
    @PutMapping("/{id}/audit")
    public Result<String> approveReservation(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Object> params,
            HttpServletRequest request) {
        String comment = params.get("auditResult").toString();
        Integer status = Integer.parseInt(params.get("auditStatus").toString());
        
        String token = request.getHeader("Authorization");
        Long auditUserId = tokenUtil.getUserIdFromToken(token);
        
        try {
            boolean approved = reservationService.approveReservation(id, auditUserId, comment, status);
            if (approved) {
                EquipmentReservation reservation = reservationService.getById(id);
                String statusText = status == 1 ? "已通过" : "已拒绝";
                String msg = "{\"type\":\"reservation_refresh\",\"message\":\"您的预约" 
                    + reservation.getEquipmentName() + "已被" + statusText + "\"}";
                WebSocketHandler.sendToAll(msg);
                return Result.success("预约审核成功");
            } else {
                return Result.error("预约审核失败");
            }
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/check-conflict")
    public Result<Boolean> checkConflict(
            @RequestParam Long equipmentId,
            @RequestParam LocalDateTime reserveTime,
            @RequestParam Integer reserveDuration,
            @RequestParam(required = false) Long excludeId) {
        boolean hasConflict = reservationService.checkConflict(equipmentId, reserveTime, reserveDuration, excludeId);
        return Result.success(hasConflict);
    }
    
    @GetMapping("/calendar")
    public Result<List<EquipmentReservationVO>> getCalendarReservations(
            @RequestParam Long equipmentId,
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        List<EquipmentReservationVO> reservations = reservationService.getReservationsForCalendar(equipmentId, start, end);
        return Result.success(reservations);
    }
}
