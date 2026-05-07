package com.swpu.equipment.lifecycle.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("equipment_reservation")
public class EquipmentReservation {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("equipment_id")
    private Long equipmentId;

    @TableField("equipment_number")
    private String equipmentNumber;

    @TableField("equipment_name")
    private String equipmentName;

    @TableField("equipment_model")
    private String equipmentModel;

    @TableField("equipment_image")
    private String equipmentImage;

    @TableField("equipment_type_id")
    private Long equipmentTypeId;

    @TableField("reserve_status")
    private Integer reserveStatus;
    // 预约状态: 0-待审核, 1-已通过, 2-已拒绝, 3-已取消

    @TableField("user_id")
    private Long userId;

    @TableField("real_name")
    private String realName;

    @TableField("reserve_time")
    private LocalDateTime reserveTime;

    @TableField("reserve_duration")
    private Integer reserveDuration;

    private String purpose;

    @TableField("phone")
    private String phone;

    @TableField("audit_status")
    private Integer auditStatus;
    // 审核状态: 0-待审核, 1-已同意, 2-已拒绝

    @TableField("audit_result")
    private String auditResult;

    @TableField("audit_user_id")
    private Long auditUserId;

    @TableField("audit_user_name")
    private String auditUserName;

    @TableField("audit_time")
    private LocalDateTime auditTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField("is_extension")
    private Integer isExtension;
}
