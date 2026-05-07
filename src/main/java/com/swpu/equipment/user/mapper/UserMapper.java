package com.swpu.equipment.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.swpu.equipment.user.entity.User;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问层
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    /**
     * 根据学号/工号/手机号查询用户
     * @param identifier 学号/工号/手机号
     * @return 用户信息
     */
    User selectByIdentifier(String identifier);

    List<Map<String, Object>> getManagerOptions();
}
