package com.swpu.equipment.warehouse.export;

import lombok.Data;

@Data
public class StockExcelData {
    private String warehouseName;
    private String supplierName;
    private String equipmentNumber;
    private String equipmentName;
    private String typeText;
    private Integer quantity;
    private String operatorName;
    private String createTime;
    private String remark;
}
