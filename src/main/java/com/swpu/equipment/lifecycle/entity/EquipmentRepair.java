package com.swpu.equipment.lifecycle.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("equipment_repair")
public class EquipmentRepair {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 设备ID（外键）
     */
    @TableField("equipment_id")
    private Long equipmentId;

    /**
     * 设备编号
     */
    @TableField("equipment_number")
    private String equipmentNumber;

    /**
     * 设备名称
     */
    @TableField("equipment_name")
    private String equipmentName;

    /**
     * 设备型号
     */
    @TableField("equipment_model")
    private String equipmentModel;

    /**
     * 设备图片URL
     */
    @TableField("equipment_image")
    private String equipmentImage;

    /**
     * 设备类型ID（外键）
     */
    @TableField("equipment_type_id")
    private Long equipmentTypeId;

    /**
     * 报修状态：0待审核 1报修中 2维修中 3已完成 4已拒绝
     */
    @TableField("repair_status")
    private Integer repairStatus;

    /**
     * 报修人ID（外键）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 报修人真实姓名
     */
    @TableField("real_name")
    private String realName;

    /**
     * 故障说明
     */
    @TableField("fault_description")
    private String faultDescription;

    /**
     * 报修人电话
     */
    @TableField("phone")
    private String phone;

    /**
     * 报修时间
     */
    @TableField("repair_time")
    private LocalDateTime repairTime;

    /**
     * 审核状态：0待审核 1已同意 2已拒绝
     */
    @TableField("audit_status")
    private Integer auditStatus;

    /**
     * 审核结果（如：同意报修）
     */
    @TableField("audit_result")
    private String auditResult;

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
