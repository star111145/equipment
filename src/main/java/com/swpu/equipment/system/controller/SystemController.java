package com.swpu.equipment.system.controller;

import com.swpu.equipment.common.util.Result;
import com.swpu.equipment.system.entity.SystemConfig;
import com.swpu.equipment.system.service.SystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/system")
public class SystemController {
    
    @Autowired
    private SystemService systemService;
    
    @GetMapping("/config")
    public Result<Map<String, Object>> getAllConfigs() {
        List<SystemConfig> allConfigs = systemService.getAllConfigs();
        
        if (allConfigs.isEmpty()) {
            systemService.initDefaultConfigs();
            allConfigs = systemService.getAllConfigs();
        }
        
        Map<String, Object> result = new HashMap<>();
        
        List<SystemConfig> reservationConfigs = allConfigs.stream()
                .filter(c -> "reservation".equals(c.getCategory()))
                .collect(Collectors.toList());
        
        List<SystemConfig> securityConfigs = allConfigs.stream()
                .filter(c -> "security".equals(c.getCategory()))
                .collect(Collectors.toList());
        
        result.put("reservation", reservationConfigs);
        result.put("security", securityConfigs);
        
        return Result.success(result);
    }
    
    @GetMapping("/config/category/{category}")
    public Result<List<SystemConfig>> getConfigsByCategory(@PathVariable String category) {
        List<SystemConfig> configs = systemService.getConfigsByCategory(category);
        return Result.success(configs);
    }
    
    @GetMapping("/config/value/{key}")
    public Result<String> getConfigValue(@PathVariable String key) {
        String value = systemService.getConfigValue(key);
        return Result.success(value);
    }
    
    @PutMapping("/config")
    public Result<Void> updateConfig(@RequestBody SystemConfig config) {
        boolean success = systemService.updateConfig(config);
        if (success) {
            return Result.success(null);
        }
        return Result.error("更新失败");
    }
    
    @PutMapping("/config/batch")
    public Result<Void> batchUpdateConfigs(@RequestBody List<SystemConfig> configs) {
        boolean success = systemService.batchUpdateConfigs(configs);
        if (success) {
            return Result.success(null);
        }
        return Result.error("批量更新失败");
    }
    
    @PostMapping("/config/init")
    public Result<Void> initDefaultConfigs() {
        systemService.initDefaultConfigs();
        return Result.success(null);
    }
}
