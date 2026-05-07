package com.swpu.equipment.equipment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("equipment")
public class Equipment {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

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
     * 设备类型
     */
    @TableField(exist = false)
    private String equipmentType;

    /**
       * 设备状态（手动标记）：0-维修中 1-空闲（默认） 4-故障
       * 说明：其他状态（2被预约、3已借用）通过动态计算得出，不使用此字段
       */
      @TableField("equipment_status")
      private Integer equipmentStatus;

    /**
     * 仓库ID（外键）
     */
    @TableField("warehouse_id")
    private Long warehouseId;
    
    /**
     * 仓库位置
     */
    @TableField(exist = false)
    private String equipmentLocation;

    /**
     * 供应商ID（外键）
     */
    @TableField("supplier_id")
    private Long supplierId;
    
    /**
     * 供应商
     */
    @TableField(exist = false)
    private String supplier;

    /**
     * 库存数量
     */
    @TableField("stock_quantity")
    private Integer stockQuantity;

    /**
     * 可用数量
     */
    @TableField("available_quantity")
    private Integer availableQuantity;

    /**
     * 借用数量（动态统计）
     */
    @TableField(exist = false)
    private Integer borrowQuantity;

    /**
     * 维修数量（动态统计）
     */
    @TableField(exist = false)
    private Integer repairQuantity;

    /**
     * 设备描述
     */
    private String description;

    /**
     * 二维码URL
     */
    @TableField("qrcode_url")
    private String qrcodeUrl;

    /**
     * 二维码内容
     */
    @TableField("qrcode_content")
    private String qrcodeContent;

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
