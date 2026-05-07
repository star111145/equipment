package com.swpu.equipment.lifecycle.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("equipment_borrow")
public class EquipmentBorrow {
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
     * 设备类型名称（非数据库字段）
     */
    @TableField(exist = false)
    private String equipmentType;

    @TableField(exist = false)
    private Integer hasReturn;
    
    @TableField(exist = false)
    private Integer hasRepairReturn;

    /**
     * 借用状态: 0-待审核, 1-已借出, 2-已完成, 3-已取消
     */
    @TableField("borrow_status")
    private Integer borrowStatus;

    /**
     * 借用人ID（外键）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 借用人真实姓名
     */
    @TableField("real_name")
    private String realName;

    /**
     * 借用数量
     */
    @TableField("borrow_quantity")
    private Integer borrowQuantity;
    
    @TableField(exist = false)
    private Integer returnQuantity;

    /**
     * 原始借用数量（非数据库字段，用于显示）
     */
    @TableField(exist = false)
    private Integer originalQuantity;

    /**
     * 用途
     */
    private String purpose;

    /**
     * 预约ID（关联预约表）
     */
    @TableField("reserve_id")
    private Long reserveId;

    /**
     * 预计归还时间
     */
    @TableField("expected_return_time")
    private LocalDateTime expectedReturnTime;

    /**
     * 借用人电话
     */
    private String phone;

    /**
     * 借用时间
     */
    @TableField("borrow_time")
    private LocalDateTime borrowTime;

    /**
     * 审核状态：0待审核 1已同意 2已拒绝
     */
    @TableField("audit_status")
    private Integer auditStatus;

    /**
     * 审核结果
     */
    @TableField("audit_result")
    private String auditResult;

    @TableField("audit_user_id")
    private Long auditUserId;

    @TableField("audit_user_name")
    private String auditUserName;

    @TableField("audit_time")
    private LocalDateTime auditTime;

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
