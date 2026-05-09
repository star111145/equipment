package com.swpu.equipment.equipment.entity;

import lombok.Data;

@Data
public class EquipmentVO {
    private Long id;
    private String equipmentNumber;
    private String equipmentName;
    private String equipmentModel;
    private String equipmentType;
    private Long equipmentTypeId;
    private Integer equipmentStatus;
    private String equipmentLocation;
    private Long warehouseId;
    private Integer stockQuantity;
    private Integer availableQuantity;
    private Integer borrowQuantity;
    private Integer unavailableQuantity;
    private Integer reserveQuantity;
    private String description;
    private String supplier;
    private Long supplierId;
    private String equipmentImage;
    private String qrcodeUrl;
    private Boolean isListed;
}
