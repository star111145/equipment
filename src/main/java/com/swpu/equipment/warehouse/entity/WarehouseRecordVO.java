package com.swpu.equipment.warehouse.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WarehouseRecordVO {
    private Long id;
    private Long equipmentId;
    private String equipmentNumber;
    private String equipmentName;
    private Long warehouseId;
    private String warehouseName;
    private Long supplierId;
    private String supplierName;
    private Integer recordType;
    private String typeText;
    private Integer quantity;
    private Integer stockQuantity;
    private Long operatorId;
    private String operatorName;
    private String remark;
    private LocalDateTime createTime;
}
