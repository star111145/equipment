package com.swpu.equipment.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swpu.equipment.system.entity.SystemConfig;
import com.swpu.equipment.system.mapper.SystemMapper;
import com.swpu.equipment.system.service.SystemService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemServiceImpl extends ServiceImpl<SystemMapper, SystemConfig> implements SystemService {
    
    private static final Map<String, String> DEFAULT_CONFIGS = new HashMap<>();
    
    static {
        DEFAULT_CONFIGS.put("reservation.max_advance_days", "30");
        DEFAULT_CONFIGS.put("reservation.max_duration_hours", "720");
        DEFAULT_CONFIGS.put("reservation.review_timeout_hours", "24");
        DEFAULT_CONFIGS.put("reservation.conflict_check_enabled", "true");
        
        DEFAULT_CONFIGS.put("security.password_min_length", "8");
        DEFAULT_CONFIGS.put("security.password_require_special_char", "true");
        DEFAULT_CONFIGS.put("security.login_max_attempts", "5");
        DEFAULT_CONFIGS.put("security.login_lockout_minutes", "30");
    }
    
    private static final Map<String, String> CONFIG_DESCRIPTIONS = new HashMap<>();
    
    static {
        CONFIG_DESCRIPTIONS.put("reservation.max_advance_days", "最多可提前多少天预约设备");
        CONFIG_DESCRIPTIONS.put("reservation.max_duration_hours", "单次预约最长时长（小时）");
        CONFIG_DESCRIPTIONS.put("reservation.review_timeout_hours", "审核时限（小时）");
        CONFIG_DESCRIPTIONS.put("reservation.conflict_check_enabled", "是否启用冲突检测");
        
        CONFIG_DESCRIPTIONS.put("security.password_min_length", "密码最小长度");
        CONFIG_DESCRIPTIONS.put("security.password_require_special_char", "密码是否需要特殊字符");
        CONFIG_DESCRIPTIONS.put("security.login_max_attempts", "登录失败最大次数");
        CONFIG_DESCRIPTIONS.put("security.login_lockout_minutes", "登录锁定时间（分钟）");
    }
    
    @Override
    public List<SystemConfig> getAllConfigs() {
        return list();
    }
    
    @Override
    public List<SystemConfig> getConfigsByCategory(String category) {
        return list(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getCategory, category));
    }
    
    @Override
    public String getConfigValue(String key) {
        SystemConfig config = getOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, key));
        return config != null ? config.getConfigValue() : null;
    }
    
    @Override
    public boolean updateConfig(SystemConfig config) {
        config.setUpdateTime(LocalDateTime.now());
        return updateById(config);
    }
    
    @Override
    public boolean batchUpdateConfigs(List<SystemConfig> configs) {
        for (SystemConfig config : configs) {
            config.setUpdateTime(LocalDateTime.now());
        }
        return updateBatchById(configs);
    }
    
    @Override
    public void initDefaultConfigs() {
        for (Map.Entry<String, String> entry : DEFAULT_CONFIGS.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            SystemConfig existing = getOne(new LambdaQueryWrapper<SystemConfig>()
                    .eq(SystemConfig::getConfigKey, key));
            
            if (existing == null) {
                SystemConfig config = new SystemConfig();
                config.setConfigKey(key);
                config.setConfigValue(value);
                config.setDescription(CONFIG_DESCRIPTIONS.get(key));
                
                String category = key.split("\\.")[0];
                config.setCategory(category);
                config.setCreateTime(LocalDateTime.now());
                config.setUpdateTime(LocalDateTime.now());
                
                save(config);
            }
        }
    }
}
