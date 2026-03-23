package com.swpu.equipment.warehouse.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("warehouse")
public class Warehouse {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 仓库名称
     */
    private String warehouseName;

    /**
     * 仓库位置
     */
    @TableField("warehouse_location")
    private String warehouseLocation;

    /**
     * 仓库管理员ID
     */
    @TableField("warehouse_manager_id")
    private Long warehouseManagerId;

    /**
     * 仓库管理员名称
     */
    @TableField(exist = false)
    private String warehouseManagerName;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 仓库描述
     */
    private String description;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
