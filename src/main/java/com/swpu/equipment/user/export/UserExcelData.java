package com.swpu.equipment.user.export;

import lombok.Data;

@Data
public class UserExcelData {
    private String username;
    private String studentId;
    private String realName;
    private String phone;
    private String email;
    private String gender;
    private String role;
    private String status;
    private String createTime;
}
