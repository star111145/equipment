package com.swpu.equipment.user.controller;

import com.swpu.equipment.common.util.PasswordEncryptUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 接口路径：/api/user（匹配yaml的context-path: /api）
@RestController
@RequestMapping("/user")
public class UserController {
    // 测试接口：http://localhost:8100/api/user/test
    @GetMapping("/test")
    public String test() {
        return "用户模块接口测试成功！";
    }



    @Autowired
    private PasswordEncryptUtil passwordEncryptUtil;

    // 测试加密：http://localhost:8100/api/user/encrypt?password=123456
    @GetMapping("/encrypt")
    public String encryptPassword(@RequestParam String password) {
        return "加密后的密码：" + passwordEncryptUtil.encrypt(password);
    }

    // 测试验证：http://localhost:8100/api/user/match?raw=123456&encoded=加密后的密码
    @GetMapping("/match")
    public boolean matchPassword(@RequestParam String raw, @RequestParam String encoded) {
        return passwordEncryptUtil.matches(raw, encoded);
    }
}
