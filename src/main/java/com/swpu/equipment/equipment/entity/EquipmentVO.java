package com.swpu.equipment.equipment.entity;

import lombok.Data;

@Data
public class EquipmentVO {
    private Long id;
    private String equipmentNumber;
    private String equipmentName;
    private String equipmentType;
    private Integer equipmentStatus;
    private String equipmentLocation;
    private Integer stockQuantity;
    private String description;
    private String supplier;
    private String equipmentImage;
    private String qrcodeUrl;
}
