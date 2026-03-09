package com.swpu.equipment.system.controller;

import com.swpu.equipment.common.annotation.RequireAdmin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 参数配置/安全管理接口
// 整个类都需要管理员权限：注解加在类上
@RestController
@RequestMapping("/system")
@RequireAdmin(message = "系统管理仅管理员可访问")
public class SystemController {
    // 该类下所有接口都需要管理员权限
    @GetMapping("/config")
    public String getSystemConfig() {
        return "系统配置信息";
    }
}
