package com.swpu.equipment.system.service;

import com.swpu.equipment.system.entity.SystemConfig;

import java.util.List;

public interface SystemService {
    
    /**
     * 获取所有系统配置
     */
    List<SystemConfig> getAllConfigs();
    
    /**
     * 根据分类获取配置
     */
    List<SystemConfig> getConfigsByCategory(String category);
    
    /**
     * 根据键名获取配置值
     */
    String getConfigValue(String key);
    
    /**
     * 更新配置
     */
    boolean updateConfig(SystemConfig config);
    
    /**
     * 批量更新配置
     */
    boolean batchUpdateConfigs(List<SystemConfig> configs);
    
    /**
     * 初始化默认配置（如果不存在）
     */
    void initDefaultConfigs();
}
