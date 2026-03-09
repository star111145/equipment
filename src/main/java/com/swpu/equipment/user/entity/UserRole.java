package com.swpu.equipment.user.entity;

import com.baomidou.mybatisplus.annotation.EnumValue; // 关键注解
import com.fasterxml.jackson.annotation.JsonValue; // 可选：返回JSON时显示value而非枚举名称

//角色枚举
public enum UserRole {
    // 枚举值与MySQL的ENUM值完全一致（小写）
    STUDENT("student", "学生"),
    TEACHER("teacher", "教师"),
    ADMIN("admin", "管理员");

    // 存储到数据库的核心值（与MySQL字段匹配）
    @EnumValue // 核心：告诉MyBatis-Plus这个字段对应数据库值
    @JsonValue // 可选：前端返回JSON时，显示value（如"admin"）而非枚举名称（ADMIN）
    private final String value;

    // 前端展示用的描述（可选）
    private final String desc;

    // 构造方法
    UserRole(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    // 获取数据库存储值
    public String getValue() {
        return value;
    }

    // 获取描述
    public String getDesc() {
        return desc;
    }

    // 核心方法：根据数据库值反序列化为枚举（避免硬编码）
    public static UserRole getByValue(String value) {
        for (UserRole role : UserRole.values()) {
            if (role.getValue().equals(value)) {
                return role;
            }
        }
        // 无匹配值时抛出异常，避免脏数据
        throw new IllegalArgumentException("无效的用户角色：" + value);
    }

    // 快捷判断是否为管理员
    public boolean isAdmin() {
        return this == ADMIN;
    }

    // 快捷判断是否为学生
    public boolean isStudent() {
        return this == STUDENT;
    }

    // 快捷判断是否为教师
    public boolean isTeacher() {
        return this == TEACHER;
    }
}