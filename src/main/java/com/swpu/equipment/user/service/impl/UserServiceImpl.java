package com.swpu.equipment.user.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swpu.equipment.user.entity.User;
import com.swpu.equipment.user.entity.UserLoginDTO;

import com.swpu.equipment.user.entity.UserRole;
import com.swpu.equipment.user.mapper.UserMapper;
import com.swpu.equipment.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 用户业务层实现（优化后）
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 完整的isAdmin方法（无截断）
    @Override
    public boolean isAdmin(Long userId) {
        Assert.notNull(userId, "用户ID不能为空");
        User user = this.getById(userId);
        Assert.notNull(user, "用户ID：" + userId + " 不存在");
        return user.isAdmin(); // 调用实体类无参方法
    }

    // 其他方法（login/getUserByUsername/saveUser）保持之前优化后的代码不变
    @Override
    public User login(UserLoginDTO loginDTO) {
        Assert.notNull(loginDTO, "登录参数不能为空");
        Assert.hasText(loginDTO.getIdentifier().trim(), "登录标识符不能为空");
        Assert.hasText(loginDTO.getPassword().trim(), "密码不能为空");

        User user = userMapper.selectByIdentifier(loginDTO.getIdentifier().trim());
        Assert.notNull(user, "用户不存在或账号已禁用");
// 校验用户状态（可选，确保未禁用）
        Assert.isTrue(1 == user.getStatus(), "账号已被禁用");
        boolean passwordMatch = passwordEncoder.matches(loginDTO.getPassword(), user.getPassword());
        Assert.isTrue(passwordMatch, "密码错误，请重新输入");
        
        // 校验登录角色
        String selectedRole = loginDTO.getRole();
        boolean userIsAdmin = user.isAdmin();
        
        // 添加日志调试
        System.out.println("【登录调试】标识符: " + loginDTO.getIdentifier());
        System.out.println("【登录调试】选择的角色: " + selectedRole);
        System.out.println("【登录调试】用户是否为管理员: " + userIsAdmin);
        
        // 如果用户是管理员，但选择了普通用户登录，提示错误
        if (userIsAdmin && "user".equals(selectedRole)) {
            throw new IllegalArgumentException("您是管理员，请使用管理员身份登录");
        }
        
        // 如果用户不是管理员，但选择了管理员登录，拒绝访问
        if (!userIsAdmin && "admin".equals(selectedRole)) {
            throw new IllegalArgumentException("您不是管理员，无权进入管理员页面");
        }
        
//清空密码
        user.setPassword(null);
        return user;
    }

    @Override
    public User getUserByIdentifier(String identifier) {
        Assert.hasText(identifier.trim(), "标识符不能为空");
        User user = userMapper.selectByIdentifier(identifier.trim());
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    @Override
    public boolean saveUser(User user) {
        // 1. 基础参数校验
        Assert.notNull(user, "用户信息不能为空");
        Assert.hasText(user.getUsername().trim(), "用户名不能为空");
        Assert.notNull(user.getPassword(), "用户密码不能为空");
        Assert.hasText(user.getPassword().trim(), "用户密码不能为空白字符");
        Assert.notNull(user.getRole(), "用户角色不能为空");

        // 2. 优化：通过枚举遍历校验，避免硬编码（新增角色时只需改枚举，无需改这里）
        boolean isRoleValid = false;
        for (UserRole role : UserRole.values()) {
            if (role.equals(user.getRole())) {
                isRoleValid = true;
                break;
            }
        }
        if (!isRoleValid) {
            throw new IllegalArgumentException("角色只能是：" + Arrays.toString(UserRole.values()));
        }

        // 3. 密码加密 + 保存
        user.setPassword(passwordEncoder.encode(user.getPassword().trim()));
        return this.save(user);
    }

    @Override
    public boolean isTeacher(Long userId) {
        Assert.notNull(userId, "用户ID不能为空");
        User user = this.getById(userId);
        Assert.notNull(user, "用户ID：" + userId + " 不存在");
        return user.isTeacher();
    }

    @Override
    public IPage<User> getUserPage(IPage<User> page, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(User::getStudentId, keyword)
                   .or()
                   .like(User::getRealName, keyword)
                   .or()
                   .like(User::getEmail, keyword)
                   .or()
                   .like(User::getPhone, keyword);
        }
        return this.page(page, wrapper);
    }


    @Override
    public List<Map<String, Object>> getManagerOptions() {
        return userMapper.getManagerOptions();
    }

}