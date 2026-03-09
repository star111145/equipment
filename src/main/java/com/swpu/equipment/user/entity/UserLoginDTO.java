package com.swpu.equipment.user.entity;
import lombok.Data;
//登录入参

import javax.validation.constraints.NotBlank;

@Data
public class UserLoginDTO {
    /**
     * 用户名（学号/工号）
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
