package com.swpu.equipment.user.controller;

import com.swpu.equipment.common.util.Result;
import com.swpu.equipment.common.util.TokenUtil;
import com.swpu.equipment.common.util.LoginAttemptManager;
import com.swpu.equipment.user.entity.User;
import com.swpu.equipment.user.entity.UserLoginDTO;
import com.swpu.equipment.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录Controller
 */
@RestController
// 关键：不配置@RequestMapping前缀，依赖context-path: /api
public class LoginController {
    // 增加Controller日志，排查请求是否到达
    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private LoginAttemptManager loginAttemptManager;

    // 强制注入TokenUtil，若注入失败会直接报错（便于排查）
    @Autowired
    private TokenUtil tokenUtil;

    // ========== 统一登录接口（最终路径：http://localhost:8100/api/login） ==========
    @PostMapping("/login")
    public Result<Map<String, Object>> login(
            @RequestBody UserLoginDTO loginDTO,
            HttpSession session) {
        // 先检查用户是否存在且未被禁用
        try {
            User checkUser = userService.getUserByIdentifier(loginDTO.getIdentifier());
            if (checkUser == null) {
                return Result.error(401, "用户不存在");
            }
            if (checkUser.getStatus() != 1) {
                return Result.error(401, "账号已被禁用，请联系管理员");
            }
        } catch (Exception e) {
            // 用户不存在的情况会在后续处理
        }
        
        // 检查登录尝试次数
        String attemptError = loginAttemptManager.checkLoginAttempt(loginDTO.getIdentifier());
        if (attemptError != null) {
            return Result.error(401, attemptError);
        }
        
        // 打印请求日志，确认请求到达Controller
        log.info("接收到登录请求：identifier={}", loginDTO.getIdentifier());
        try {
            // 1. 调用Service校验数据库用户
            User user = userService.login(loginDTO);
            log.info("用户登录成功：identifier={}, userId={}", user.getStudentId(), user.getId());

            // 2. 生成Token（强制打印日志）
            String token = tokenUtil.generateToken(user.getId());
            log.info("生成Token成功：{}", token); // 手动打印，不依赖TokenUtil内部日志

            // 3. 组装返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("userId", user.getId());
            data.put("username", user.getUsername());
            data.put("realName", user.getRealName());
            data.put("phone", user.getPhone());
            data.put("role", user.getRole());
            data.put("isAdministrator", user.isAdmin());
            data.put("tokenExpireTime", System.currentTimeMillis() + tokenUtil.getExpire());

            // 4. 存入Session
            session.setAttribute("loginUser", user);
            
            // 5. 记录登录成功，清除失败次数
            loginAttemptManager.recordSuccess(loginDTO.getIdentifier());

            return Result.success(data);
        } catch (IllegalArgumentException e) {
            log.error("登录失败（业务异常）：{}", e.getMessage());
            // 记录登录失败
            loginAttemptManager.recordFailure(loginDTO.getIdentifier());
            return Result.error(401, e.getMessage());
        } catch (Exception e) {
            log.error("登录失败（系统异常）", e); // 打印完整异常堆栈
            // 记录登录失败
            loginAttemptManager.recordFailure(loginDTO.getIdentifier());
            return Result.error(500, "登录失败：" + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        try {
            session.removeAttribute("loginUser");
         log.info("用户退出登录成功");   
         return Result.success();      
        } catch (Exception e) {
            log.error("退出登录失败", e);
            return Result.error("退出登录失败：" + e.getMessage());
        }
    }

    @GetMapping("/test")
    public Result<String> test() {
        log.info("测试接口被调用");
        return Result.success("测试接口返回成功");
    }
}