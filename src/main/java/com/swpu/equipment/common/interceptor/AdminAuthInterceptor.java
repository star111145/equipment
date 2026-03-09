package com.swpu.equipment.common.interceptor;

import com.swpu.equipment.common.annotation.RequireAdmin;
import com.swpu.equipment.common.util.TokenUtil;
import com.swpu.equipment.user.entity.User;
import com.swpu.equipment.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;

/**
 * 权限拦截器：校验管理员权限
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenUtil tokenUtil;

    /**
     * 请求处理前执行：核心权限校验逻辑
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 判断是否是控制器方法（排除静态资源等）
        if (!(handler instanceof HandlerMethod)) {
            return true; // 非接口请求，直接放行
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 2. 判断接口是否需要管理员权限（检查@RequireAdmin注解）
        RequireAdmin requireAdmin = handlerMethod.getMethodAnnotation(RequireAdmin.class);
        if (requireAdmin == null) {
            // 类上的注解（若接口类整体需要管理员权限）
            requireAdmin = handlerMethod.getBeanType().getAnnotation(RequireAdmin.class);
        }
        if (requireAdmin == null) {
            return true; // 无需管理员权限，放行
        }

        // 3. 获取当前登录用户ID（核心：需结合你的登录逻辑，这里提供2种常见方式）
        Long currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            // 未登录：返回401未授权
            responseError(response, HttpStatus.UNAUTHORIZED.value(), "请先登录");
            return false;
        }

        // 4. 校验是否为管理员
        User currentUser = userService.getById(currentUserId);
        if (currentUser == null || !currentUser.isAdmin()) { // 调用User实体类的isAdmin()
            // 非管理员：返回403禁止访问
            responseError(response, HttpStatus.FORBIDDEN.value(), requireAdmin.message());
            return false;
        }

        // 5. 权限校验通过，放行
        return true;
    }

    /**
     * 从请求中获取当前登录用户ID（适配2种常见登录方式，选其一即可）
     * 方式1：Session登录（简单场景）
     * 方式2：Token登录（前后端分离，推荐）
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        // ========== 方式1：Session登录（适合单体应用） ==========
        // User loginUser = (User) request.getSession().getAttribute("loginUser");
        // return loginUser != null ? loginUser.getId() : null;

        // ========== 方式2：Token登录（适合前后端分离，推荐） ==========
        String token = request.getHeader("Authorization"); // 前端请求头携带Token
        if (token == null || token.isEmpty()) {
            return null;
        }
        // 解析Token获取用户ID（需实现Token工具类，下方提供示例）
        return tokenUtil.getUserIdFromToken(token);
    }

    /**
     * 响应错误信息给前端
     */
    private void responseError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write("{\"code\":" + status + ",\"message\":\"" + message + "\"}");
        writer.flush();
        writer.close();
    }
}