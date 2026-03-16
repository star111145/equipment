package com.swpu.equipment.user.entity;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UserLoginDTO {
    /**
     * 登录标识符（学号/工号/手机号）
     */
    @NotBlank(message = "登录标识符不能为空")
    private String identifier;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 登录角色类型：user(普通用户) 或 admin(管理员)
     */
    @NotBlank(message = "请选择登录角色")
    private String role;
}
