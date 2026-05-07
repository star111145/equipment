package com.swpu.equipment.lifecycle.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

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
     * 设备类型名称（非数据库字段，通过查询获取）
     */
    @TableField(exist = false)
    private String equipmentTypeName;

    /**
     * 报修状态：0待审核 1报修中 2已维修 3已拒绝 4已取消
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
     * 报修数量
     */
    @TableField("repair_quantity")
    private Integer repairQuantity;

    /**
     * 故障说明
     */
    @TableField("fault_description")
    private String faultDescription;

    /**
     * 故障图片URL列表（JSON格式字符串）
     */
    @TableField("fault_image")
    private String faultImageJson;

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
     * 预计归还时间（借用中的设备报修时）
     */
    @TableField("expected_return_time")
    private LocalDateTime expectedReturnTime;

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

    /**
     * 故障图片列表（从JSON字符串解析）
     */
    public List<String> getFaultImageList() {
        if (faultImageJson == null || faultImageJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(faultImageJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 设置故障图片列表（转换为JSON字符串）
     */
    public void setFaultImageList(List<String> faultImageList) {
        if (faultImageList == null || faultImageList.isEmpty()) {
            this.faultImageJson = null;
            return;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.faultImageJson = mapper.writeValueAsString(faultImageList);
        } catch (Exception e) {
            this.faultImageJson = null;
        }
    }
}
