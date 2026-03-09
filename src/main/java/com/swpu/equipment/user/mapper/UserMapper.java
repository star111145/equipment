package com.swpu.equipment.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.swpu.equipment.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问层
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    /**
     * 根据用户名查询用户
     * @param username 用户名（学号/工号）
     * @return 用户信息
     */
    User selectByUsername(String username);
}
