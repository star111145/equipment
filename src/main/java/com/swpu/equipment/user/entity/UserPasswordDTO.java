package com.swpu.equipment.user.entity;

import lombok.Data;

@Data
public class UserPasswordDTO {
    private String oldPassword;
    private String newPassword;
}
