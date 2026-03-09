package com.swpu.equipment.user.controller;

import com.swpu.equipment.common.annotation.RequireAdmin;
import com.swpu.equipment.common.result.Result;
import com.swpu.equipment.common.util.PasswordEncryptUtil;
import com.swpu.equipment.common.util.TokenUtil;
import com.swpu.equipment.user.entity.User;
import com.swpu.equipment.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 接口路径：/api/user（匹配yaml的context-path: /api）
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private TokenUtil tokenUtil;

    // 个人信息接口：获取当前登录用户的完整信息
    @GetMapping("/current/info")
    public Result<User> getCurrentUserInfo(HttpServletRequest request) {
        // 1. 从请求头获取Token
        String token = request.getHeader("Authorization");
        // 2. 解析Token获取用户ID
        Long userId = tokenUtil.getUserIdFromToken(token);
        // 3. 查询用户完整信息（排除密码）
        User user = userService.getById(userId);//使用MyBatis的方法
        user.setPassword(null); // 隐藏敏感信息
        // 4. 返回完整信息
        return Result.success(user);
    }


    // 普通接口：所有登录用户可访问（无需注解）
    @GetMapping("/info/{userId}")
    public User getUserInfo(@PathVariable Long userId) {
        return userService.getById(userId);
    }

    // 管理员专属接口：添加@RequireAdmin注解
    @PostMapping("/add")
    @RequireAdmin(message = "新增用户仅管理员可操作")
    public boolean addUser(@RequestBody User user) {
        return userService.saveUser(user);
    }

    // 管理员专属接口：批量删除用户
    @DeleteMapping("/batch")
    @RequireAdmin
    public boolean batchDelete(@RequestParam List<Long> userIds) {
        return userService.removeByIds(userIds);
    }


//
//    // 测试接口：http://localhost:8100/api/user/test
//    @GetMapping("/test")
//    public String test() {
//        return "用户模块接口测试成功！";
//    }
//
//
//
//    @Autowired
//    private PasswordEncryptUtil passwordEncryptUtil;
//
//    // 测试加密：http://localhost:8100/api/user/encrypt?password=123456
//    @GetMapping("/encrypt")
//    public String encryptPassword(@RequestParam String password) {
//        return "加密后的密码：" + passwordEncryptUtil.encrypt(password);
//    }
//
//    // 测试验证：http://localhost:8100/api/user/match?raw=123456&encoded=加密后的密码
//    @GetMapping("/match")
//    public boolean matchPassword(@RequestParam String raw, @RequestParam String encoded) {
//        return passwordEncryptUtil.matches(raw, encoded);
//    }




}
