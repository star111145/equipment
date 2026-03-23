package com.swpu.equipment.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swpu.equipment.user.entity.User;
import com.swpu.equipment.user.entity.UserLoginDTO;


/**
 * 用户业务层接口
 */
public interface UserService extends IService<User> {
    /**
     * 用户登录
     * @param loginDTO 登录参数
     * @return 登录成功的用户信息
     */
    User login(UserLoginDTO loginDTO);

    /**
     * 校验用户是否为管理员
     * @param userId 用户ID
     * @return true=管理员 false=普通用户
     */
    boolean isAdmin(Long userId);

    /**
     * 根据标识符查询用户
     * @param identifier 学号/工号/手机号
     * @return 用户信息
     */
    User getUserByIdentifier(String identifier);


    boolean saveUser(User user);


    boolean isTeacher(Long userId);

    /**
     * 分页查询用户列表
     * @param page 分页对象
     * @param keyword 搜索关键词
     * @return 分页结果
     */
    IPage<User> getUserPage(IPage<User> page, String keyword);
}