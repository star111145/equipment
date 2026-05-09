package com.swpu.equipment.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("system_config")
public class SystemConfig {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 参数键名
     */
    private String configKey;
    
    /**
     * 参数值
     */
    private String configValue;
    
    /**
     * 参数描述
     */
    private String description;
    
    /**
     * 参数分类：reservation-预约规则, security-安全管理
     */
    private String category;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
