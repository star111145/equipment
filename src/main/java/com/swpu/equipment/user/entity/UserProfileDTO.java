package com.swpu.equipment.user.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserProfileDTO {
    private String username;
    private String studentId;
    private String realName;
    private String email;
    private String phone;
    private Integer gender;
    private String avatar;
    private String role;
    private Integer status;
    private LocalDateTime registerTime;
    private LocalDateTime updateTime;
    private Boolean isAdministrator;
}
