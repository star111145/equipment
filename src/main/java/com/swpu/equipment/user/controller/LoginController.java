package com.swpu.equipment.user.controller;

import org.springframework.web.bind.annotation.*;

/**
 * 自定义登录页Controller
 */
@RestController
@RequestMapping("") // 为匹配context-path: /api，或者不写避免重复
public class LoginController {

    // 匹配自定义登录页路径 /login
    @GetMapping("/login")
    public String defaultLoginPage() {
        // 简单返回登录提示（毕设中可替换为HTML登录页）
        return "<h1>自定义登录页</h1>" +
                "<form action='/api/login' method='post'>" +
                "用户名：<input type='text' name='username'><br>" +
                "密码：<input type='password' name='password'><br>" +
                "<button type='submit'>登录</button>" +
                "</form>";
    }

    // 登录提交接口（POST）
    @PostMapping("/login")
    public String loginSubmit(@RequestParam String username, @RequestParam String password) {
        if ("admin".equals(username) && "123456".equals(password)) {
            return "登录成功！<br><a href='/api/user/test'>访问用户测试接口</a>";
        } else {
            return "用户名或密码错误！<br><a href='/api/login'>返回登录页</a>";
        }
    }
}