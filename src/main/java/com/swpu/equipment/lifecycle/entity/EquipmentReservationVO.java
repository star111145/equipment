package com.swpu.equipment.lifecycle.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EquipmentReservationVO {
    private Long id;
    private Long equipmentId;
    private String equipmentName;
    private String equipmentNumber;
    private String equipmentModel;
    private String equipmentImage;
    private String equipmentType;
    private Long userId;
    private String userName;
    private String userPhone;
    private LocalDateTime reserveTime;
    private Integer reserveDuration;
    private String purpose;
    private Integer reserveStatus;
    private Integer auditStatus;
    private Long auditUserId;
    private String auditUserName;
    private LocalDateTime auditTime;
    private String auditResult;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer hasBorrow;
    private Integer availableQuantity;
    private Integer isExtension;
}
