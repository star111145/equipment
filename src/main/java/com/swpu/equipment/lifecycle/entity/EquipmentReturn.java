package com.swpu.equipment.lifecycle.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("equipment_return")
public class EquipmentReturn {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

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
     * 归还状态：0待审核 1已归还 2已拒绝
     */
    @TableField("return_status")
    private Integer returnStatus;

    /**
     * 归还人ID（外键）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 归还人真实姓名
     */
    @TableField("real_name")
    private String realName;

    /**
     * 归还数量
     */
    @TableField("return_quantity")
    private Integer returnQuantity;

    /**
     * 归还时间
     */
    @TableField("return_time")
    private LocalDateTime returnTime;

    /**
     * 审核状态：0待审核 1已同意 2已拒绝
     */
    @TableField("audit_status")
    private Integer auditStatus;

    /**
     * 审核结果（如：归还成功）
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
