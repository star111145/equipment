package com.swpu.equipment.lifecycle.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("equipment_return_check")
public class EquipmentReturnCheck {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 归还记录ID（外键）
     */
    @TableField("return_id")
    private Long returnId;

    /**
     * 借用记录ID（外键）
     */
    @TableField("borrow_id")
    private Long borrowId;

    /**
     * 设备ID（外键）
     */
    @TableField("equipment_id")
    private Long equipmentId;

    /**
     * 设备类型ID（外键）
     */
    @TableField("equipment_type_id")
    private Long equipmentTypeId;

    /**
     * 检查状态：0待检查 1已检查
     */
    @TableField("check_status")
    private Integer checkStatus;

    /**
     * 检查结果（如：设备完好）
     */
    @TableField("check_result")
    private String checkResult;

    /**
     * 检查说明
     */
    @TableField("check_description")
    private String checkDescription;

    /**
     * 检查人ID（外键，管理员）
     */
    @TableField("checker_id")
    private Long checkerId;

    /**
     * 检查人姓名
     */
    @TableField("checker_name")
    private String checkerName;

    /**
     * 检查时间
     */
    @TableField("check_time")
    private LocalDateTime checkTime;

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
