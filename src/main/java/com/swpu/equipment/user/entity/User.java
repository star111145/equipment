package com.swpu.equipment.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user") // 对应数据库表名
public class User {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 学号/工号（登录账号）
     */
    @TableField("student_id")
    private String studentId;

    /**
     * 用户名（昵称）
     */
    private String username;

    /**
     * 加密密码
     */
    private String password;

    /**
     * 真实姓名
     */
    @TableField("real_name")
    private String realName;

    /**
     * 性别：1男 2女 0未知
     */
    private Integer gender;

    /**
     * 头像URL
     */
    private String avatar;

    // 核心：枚举字段，MyBatis-Plus会自动转换为String存储
    private UserRole role; // 数据库中存储的是"student"/"teacher"/"admin"

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 状态：1正常 0禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // 登录/权限校验时的角色判断,无参
    public boolean isAdmin() {
        return UserRole.ADMIN.equals(this.getRole());
    }

    public boolean isTeacher() {
        return UserRole.TEACHER.equals(this.getRole());
    }

    public boolean isStudent() {
        return UserRole.STUDENT.equals(this.getRole());
    }

}
