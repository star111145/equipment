package com.swpu.equipment.equipment.export;

import lombok.Data;



@Data
public class EquipmentExcelData {
    private String equipmentNumber;
    private String equipmentName;
    private String equipmentType;
    private String equipmentStatus;
    private String equipmentLocation;
    private Integer stockQuantity;
    private String supplier;
    private String description;
}
