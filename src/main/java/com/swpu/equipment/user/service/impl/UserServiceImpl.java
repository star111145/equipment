package com.swpu.equipment.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swpu.equipment.user.entity.User;
import com.swpu.equipment.user.entity.UserLoginDTO;

import com.swpu.equipment.user.entity.UserRole;
import com.swpu.equipment.user.mapper.UserMapper;
import com.swpu.equipment.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Arrays;

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
        Assert.hasText(loginDTO.getUsername().trim(), "用户名不能为空");
        Assert.hasText(loginDTO.getPassword().trim(), "密码不能为空");

        User user = userMapper.selectByUsername(loginDTO.getUsername().trim());
        Assert.notNull(user, "用户名不存在或账号已禁用");
// 校验用户状态（可选，确保未禁用）
        Assert.isTrue(1 == user.getStatus(), "账号已被禁用");
        boolean passwordMatch = passwordEncoder.matches(loginDTO.getPassword(), user.getPassword());
        Assert.isTrue(passwordMatch, "密码错误，请重新输入");
//清空密码
        user.setPassword(null);
        return user;
    }

    @Override
    public User getUserByUsername(String username) {
        Assert.hasText(username.trim(), "用户名不能为空");
        User user = userMapper.selectByUsername(username.trim());
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
}