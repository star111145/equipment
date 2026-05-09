package com.swpu.equipment.common.util;

import com.swpu.equipment.system.service.SystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ReservationValidator {
    
    @Autowired
    private SystemService systemService;
    
    public String validateReservation(LocalDateTime reserveTime, Integer duration) {
        if (reserveTime == null) {
            return "预约时间不能为空";
        }
        
        int maxAdvanceDays = 30;
        int maxDurationHours = 720;
        
        try {
            String maxAdvanceStr = systemService.getConfigValue("reservation.max_advance_days");
            String maxDurationStr = systemService.getConfigValue("reservation.max_duration_hours");
            
            if (maxAdvanceStr != null) {
                maxAdvanceDays = Integer.parseInt(maxAdvanceStr);
            }
            if (maxDurationStr != null) {
                maxDurationHours = Integer.parseInt(maxDurationStr);
            }
        } catch (Exception e) {
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxAdvanceTime = now.plusDays(maxAdvanceDays);
        
        if (reserveTime.isBefore(now)) {
            return "预约时间不能早于当前时间";
        }
        
        if (reserveTime.isAfter(maxAdvanceTime)) {
            return "只能预约" + maxAdvanceDays + "天内的设备";
        }
        
        if (duration != null && duration > maxDurationHours) {
            return "单次预约时长不能超过" + maxDurationHours + "小时";
        }
        
        return null;
    }
    
    public boolean isConflictCheckEnabled() {
        try {
            String enabled = systemService.getConfigValue("reservation.conflict_check_enabled");
            return "true".equalsIgnoreCase(enabled);
        } catch (Exception e) {
            return true;
        }
    }
    
    public int getMaxDurationHours() {
        try {
            String maxDurationStr = systemService.getConfigValue("reservation.max_duration_hours");
            if (maxDurationStr != null) {
                return Integer.parseInt(maxDurationStr);
            }
        } catch (Exception e) {
        }
        return 720;
    }
    
    public int getReviewTimeoutHours() {
        try {
            String timeoutStr = systemService.getConfigValue("reservation.review_timeout_hours");
            if (timeoutStr != null) {
                return Integer.parseInt(timeoutStr);
            }
        } catch (Exception e) {
        }
        return 24;
    }
    
    public boolean isReviewTimeoutEnabled() {
        try {
            String timeoutStr = systemService.getConfigValue("reservation.review_timeout_hours");
            if (timeoutStr != null) {
                int timeout = Integer.parseInt(timeoutStr);
                return timeout > 0;
            }
        } catch (Exception e) {
        }
        return true;
    }
}
