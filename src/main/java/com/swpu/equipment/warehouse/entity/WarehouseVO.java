package com.swpu.equipment.warehouse.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WarehouseVO {
    private Long id;
    private String warehouseName;
    private String warehouseLocation;
    private Long warehouseManagerId;
    private String warehouseManagerName;
    private String phone;
    private String description;
    private LocalDateTime createTime;
}
