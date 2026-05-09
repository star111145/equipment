package com.swpu.equipment.warehouse.export;

import lombok.Data;

@Data
public class WarehouseExcelData {
    private String warehouseName;
    private String warehouseLocation;
    private String warehouseManagerName;
    private String phone;
    private String description;
    private String createTime;
}
