package com.swpu.equipment.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.swpu.equipment.common.annotation.RequireAdmin;
import com.swpu.equipment.common.result.Result;
import com.swpu.equipment.common.util.PasswordEncryptUtil;
import com.swpu.equipment.common.util.TokenUtil;
import com.swpu.equipment.user.entity.User;
import com.swpu.equipment.user.entity.UserProfileDTO;
import com.swpu.equipment.user.entity.UserPasswordDTO;
import com.swpu.equipment.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

// 接口路径：/api/user（匹配yaml的context-path: /api）
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private TokenUtil tokenUtil;
    @Autowired
    private PasswordEncryptUtil passwordEncryptUtil;

    @GetMapping("/test")
    public String test() {
        return "用户模块接口测试成功！";
    }

    // // 个人信息接口：获取当前登录用户的完整信息
    // @GetMapping("/current/info")
    // public Result<User> getCurrentUserInfo(HttpServletRequest request) {
    //     // 1. 从请求头获取Token
    //     String token = request.getHeader("Authorization");
    //     // 2. 解析Token获取用户ID
    //     Long userId = tokenUtil.getUserIdFromToken(token);
    //     // 3. 查询用户完整信息（排除密码）
    //     User user = userService.getById(userId);//使用MyBatis的方法
    //     user.setPassword(null); // 隐藏敏感信息
    //     // 4. 返回完整信息
    //     return Result.success(user);
    // }

    // 获取当前用户的基本信息（用于个人中心）
    @GetMapping("/profile")
    public Result<UserProfileDTO> getProfile(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        User user = userService.getById(userId);
        
        Assert.notNull(user, "用户不存在");
        
        UserProfileDTO profile = new UserProfileDTO();
        profile.setUsername(user.getUsername());
        profile.setStudentId(user.getStudentId());
        profile.setRealName(user.getRealName());
        profile.setEmail(user.getEmail());
        profile.setPhone(user.getPhone());
        profile.setGender(user.getGender());
        profile.setAvatar(user.getAvatar());
        profile.setRole(user.getRole() != null ? user.getRole().name().toLowerCase() : "student");
        profile.setStatus(user.getStatus());
        profile.setRegisterTime(user.getCreateTime());
        profile.setUpdateTime(user.getUpdateTime());
        profile.setIsAdministrator(user.isAdmin());
        
        return Result.success(profile);
    }

    // 更新个人信息（普通用户只能修改用户名、邮箱、头像、性别）
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody UserProfileDTO profile, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        User user = userService.getById(userId);
        
        user.setUsername(profile.getUsername());
        user.setEmail(profile.getEmail());
        user.setGender(profile.getGender());
        user.setAvatar(profile.getAvatar());
        
        userService.updateById(user);
        
        return Result.success();
    }

    // 管理员更新个人信息
    @PutMapping("/admin/profile")
    @RequireAdmin
    public Result<Void> updateAdminProfile(@RequestBody UserProfileDTO profile, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        User user = userService.getById(userId);
        
        user.setUsername(profile.getUsername());
        user.setStudentId(profile.getStudentId());
        user.setRealName(profile.getRealName());
        user.setEmail(profile.getEmail());
        user.setPhone(profile.getPhone());
        user.setGender(profile.getGender());
        user.setAvatar(profile.getAvatar());
        
        userService.updateById(user);
        
        return Result.success();
    }

    // 修改密码（所有用户都可以修改自己的密码）
    @PutMapping("/password")
    public Result<Void> updatePassword(@RequestBody UserPasswordDTO passwordDTO, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        User user = userService.getById(userId);
        
        // 验证原密码
        if (!passwordEncryptUtil.matches(passwordDTO.getOldPassword(), user.getPassword())) {
            return Result.error("原密码错误");
        }
        
        // 设置新密码
        user.setPassword(passwordEncryptUtil.encrypt(passwordDTO.getNewPassword()));
        userService.updateById(user);
        
        return Result.success();
    }

    // 普通接口：所有登录用户可访问（无需注解）
    @GetMapping("/info/{userId}")
    public User getUserInfo(@PathVariable Long userId) {
        return userService.getById(userId);
    }

    // 管理员专属接口：添加@RequireAdmin注解
    @PostMapping("/add")
    @RequireAdmin(message = "新增用户仅管理员可操作")
    public Result<Boolean> addUser(@RequestBody User user) {
        try {
            boolean success = userService.saveUser(user);
            return success ? Result.success(true) : Result.error("添加失败");
        } catch (Exception e) {
            return Result.error("添加失败: " + e.getMessage());
        }
    }

    // 管理员专属接口：删除单个用户
    @DeleteMapping("/{userId}")
    @RequireAdmin
    public Result<Void> deleteUser(@PathVariable Long userId) {
        try {
            boolean success = userService.removeById(userId);
            return success ? Result.success() : Result.error("删除失败");
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    // 管理员专属接口：批量删除用户
    @DeleteMapping("/batch")
    @RequireAdmin
    public boolean batchDelete(@RequestParam List<Long> userIds) {
        return userService.removeByIds(userIds);
    }

    // 管理员专属接口：获取用户列表（分页）
    @GetMapping("/list")
    @RequireAdmin
    public Result<IPage<User>> getUserList(@RequestParam(defaultValue = "1") Integer current,
                                         @RequestParam(defaultValue = "10") Integer size,
                                         @RequestParam(required = false) String keyword) {
        Page<User> page = new Page<>(current, size);
        
        // 如果有搜索关键词，使用条件查询
        if (keyword != null && !keyword.trim().isEmpty()) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(User::getStudentId, keyword)
                   .or()
                   .like(User::getRealName, keyword)
                   .or()
                   .like(User::getEmail, keyword)
                   .or()
                   .like(User::getPhone, keyword);
            IPage<User> result = userService.page(page, wrapper);
            return Result.success(result);
        }
        
        // 无搜索关键词，返回全部用户
        IPage<User> result = userService.page(page, null);
        return Result.success(result);
    }

    // 管理员专属接口：获取所有用户ID（用于全选功能）
    @GetMapping("/all-ids")
    @RequireAdmin
    public Result<List<Long>> getAllUserIds() {
        try {
            List<User> users = userService.list();
            List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
            return Result.success(userIds);
        } catch (Exception e) {
            return Result.error("获取用户ID列表失败: " + e.getMessage());
        }
    }

    // 管理员专属接口：获取用户详情
    @GetMapping("/{userId}")
    @RequireAdmin
    public Result<User> getUserDetail(@PathVariable Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    // 管理员专属接口：更新用户信息
    @PutMapping("/{userId}")
    @RequireAdmin
    public Result<Void> updateUser(@PathVariable Long userId, @RequestBody User user) {
        user.setId(userId);
        try {
            boolean success = userService.updateById(user);
            return success ? Result.success() : Result.error("更新失败");
        } catch (Exception e) {
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    // 管理员专属接口：重置用户密码
    @PutMapping("/{userId}/reset-password")
    @RequireAdmin
    public Result<Void> resetPassword(@PathVariable Long userId, @RequestBody UserPasswordDTO passwordDTO) {
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPassword(passwordEncryptUtil.encrypt(passwordDTO.getNewPassword()));
        boolean success = userService.updateById(user);
        return success ? Result.success() : Result.error("重置失败");
    }

    // 管理员专属接口：批量添加用户
    @PostMapping("/batch")
    @RequireAdmin
    public Result<Void> batchAddUsers(@RequestBody List<User> users) {
        boolean success = userService.saveBatch(users);
        return success ? Result.success() : Result.error("添加失败");
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
