package com.swpu.equipment.user.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swpu.equipment.common.util.Result;
import com.swpu.equipment.common.util.PasswordEncryptUtil;
import com.swpu.equipment.common.util.PasswordValidator;
import com.swpu.equipment.common.util.TokenUtil;
import com.swpu.equipment.user.entity.User;
import com.swpu.equipment.user.entity.UserProfileDTO;
import com.swpu.equipment.user.entity.UserPasswordDTO;
import com.swpu.equipment.user.export.UserExcelData;
import com.swpu.equipment.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    @Autowired
    private PasswordValidator passwordValidator;
    @Value("${upload.base-dir:src/main/resources/static/uploads}")
    private String uploadBaseDir;

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
        if (userId == null) {
            return Result.error("无效的token");
        }
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
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
        if (userId == null) {
            return Result.error("无效的token");
        }
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        user.setUsername(profile.getUsername());
        user.setEmail(profile.getEmail());
        user.setGender(profile.getGender());
        user.setAvatar(profile.getAvatar());
        
        userService.updateById(user);
        
        return Result.success();
    }

    // 管理员更新个人信息
    @PutMapping("/admin/profile")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Void> updateAdminProfile(@RequestBody UserProfileDTO profile, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("无效的token");
        }
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
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
        if (userId == null) {
            return Result.error("无效的token");
        }
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        // 验证原密码
        if (!passwordEncryptUtil.matches(passwordDTO.getOldPassword(), user.getPassword())) {
            return Result.error("原密码错误");
        }
        
        // 验证新密码强度
        String validationError = passwordValidator.validatePassword(passwordDTO.getNewPassword());
        if (validationError != null) {
            return Result.error(validationError);
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
    @PreAuthorize("hasAuthority('admin')")
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
    @PreAuthorize("hasAuthority('admin')")
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
    @PreAuthorize("hasAuthority('admin')")
    public boolean batchDelete(@RequestParam List<Long> userIds) {
        return userService.removeByIds(userIds);
    }

    // 管理员专属接口：获取用户列表（分页）
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('admin')")
    public Result<IPage<User>> getUserList(@RequestParam(defaultValue = "1") Integer current,
                                         @RequestParam(defaultValue = "10") Integer size,
                                         @RequestParam(required = false) String keyword) {
        Page<User> page = new Page<>(current, size);
        IPage<User> result = userService.getUserPage(page, keyword);
        return Result.success(result);
    }

    // 管理员专属接口：获取所有用户ID（用于全选功能）
    @GetMapping("/all-ids")
    @PreAuthorize("hasAuthority('admin')")
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
    @PreAuthorize("hasAuthority('admin')")
    public Result<User> getUserDetail(@PathVariable Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    // 管理员专属接口：更新用户信息
    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('admin')")
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
    @PreAuthorize("hasAuthority('admin')")
    public Result<Void> resetPassword(@PathVariable Long userId, @RequestBody UserPasswordDTO passwordDTO) {
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        // 验证密码强度
        String validationError = passwordValidator.validatePassword(passwordDTO.getNewPassword());
        if (validationError != null) {
            return Result.error(validationError);
        }
        
        user.setPassword(passwordEncryptUtil.encrypt(passwordDTO.getNewPassword()));
        boolean success = userService.updateById(user);
        return success ? Result.success() : Result.error("重置失败");
    }

    // // 管理员专属接口：禁用/启用用户
    // @PutMapping("/{userId}/toggle-status")
    // @RequireAdmin
    // public Result<Void> toggleStatus(@PathVariable Long userId, HttpServletRequest request) {
    //     // 1. 获取当前登录用户（防止禁用自己）
    //     String token = request.getHeader("Authorization");
    //     Long currentUserId = tokenUtil.getUserIdFromToken(token);
    //     if (currentUserId == null) {
    //         return Result.error("未登录");
    //     }
        
    //     if (currentUserId.equals(userId)) {
    //         return Result.error("不能禁用自己");
    //     }
        
    //     User user = userService.getById(userId);
    //     if (user == null) {
    //         return Result.error("用户不存在");
    //     }
        
    //     // 2. 不能禁用管理员
    //     if (user.isAdmin()) {
    //         return Result.error("不能禁用管理员");
    //     }
        
    //     user.setStatus(user.getStatus() == 1 ? 0 : 1);
    //     boolean success = userService.updateById(user);
    //     return success ? Result.success() : Result.error("操作失败");
    // }

    // 管理员专属接口：批量添加用户
    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Void> batchAddUsers(@RequestBody List<User> users) {
        boolean success = userService.saveBatch(users);
        return success ? Result.success() : Result.error("添加失败");
    }

    //上传用户头像
    @PostMapping("/upload-avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            // 使用配置文件中的上传目录
            String uploadDir = Paths.get(uploadBaseDir, "users").toString();
            File dir = new File(uploadDir);
            System.out.println("上传目录: " + dir.getAbsolutePath());
            System.out.println("目录是否存在: " + dir.exists());
            if (!dir.exists()) {
                System.out.println("创建目录: " + dir.mkdirs());
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return Result.error("文件名不能为空");
            }

            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;

            File uploadedFile = new File(dir, newFilename);
            System.out.println("上传文件: " + uploadedFile.getAbsolutePath());
            System.out.println("文件是否存在: " + uploadedFile.exists());
            // 复制文件到目标目录
            // 代替file.transferTo(uploadedFile);
            Files.copy(file.getInputStream(), uploadedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("文件复制后是否存在: " + uploadedFile.exists());

            String avatarUrl = "/uploads/users/" + newFilename;
            System.out.println("返回的 avatarUrl: " + avatarUrl);
            return Result.success(avatarUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("上传失败: " + e.getMessage());
        }
    }

     /**
     * 获取管理员列表
     */
    @GetMapping("/managers")
    @PreAuthorize("hasAuthority('admin')")
    public Result<List<Map<String, Object>>> getManagers() {
        List<Map<String, Object>> list = userService.getManagerOptions();
        return Result.success(list);
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

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('admin')")
    public void exportUser(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") Boolean exportAll,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletResponse response) throws IOException {
        List<User> userList = userService.list();
        
        if (keyword != null && !keyword.isEmpty()) {
            userList = userList.stream()
                .filter(u -> u.getUsername().contains(keyword) || 
                           (u.getRealName() != null && u.getRealName().contains(keyword)))
                .collect(Collectors.toList());
        }
        
        if (exportAll == null) {
            exportAll = false;
        }
        
        if (!exportAll) {
            int total = userList.size();
            int fromIndex = (current - 1) * size;
            int toIndex = Math.min(fromIndex + size, total);
            if (fromIndex < total) {
                userList = userList.subList(fromIndex, toIndex);
            } else {
                userList = List.of();
            }
        }
        
        List<UserExcelData> dataList = userList.stream().map(user -> {
            UserExcelData data = new UserExcelData();
            data.setUsername(user.getUsername());
            data.setStudentId(user.getStudentId());
            data.setRealName(user.getRealName());
            data.setPhone(user.getPhone());
            data.setEmail(user.getEmail());
            data.setGender(user.getGender() != null && user.getGender() == 1 ? "男" : "女");
            String roleText = "用户";
            if (user.getRole() != null) {
                if ("admin".equals(user.getRole().getValue())) {
                    roleText = "管理员";
                } else if ("teacher".equals(user.getRole().getValue())) {
                    roleText = "教师";
                } else if ("student".equals(user.getRole().getValue())) {
                    roleText = "学生";
                }
            }
            data.setRole(roleText);
            data.setStatus(user.getStatus() != null && user.getStatus() == 1 ? "正常" : "禁用");
            data.setCreateTime(user.getCreateTime() != null ? user.getCreateTime().toString() : "");
            return data;
        }).collect(Collectors.toList());
        
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = "用户信息_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), UserExcelData.class).sheet("用户信息").doWrite(dataList);
    }

}
