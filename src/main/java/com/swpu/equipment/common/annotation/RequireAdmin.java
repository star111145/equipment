package com.swpu.equipment.common.annotation;

import java.lang.annotation.*;

/**
 * 自定义注解：标记需要管理员权限的接口
 */
@Target({ElementType.METHOD, ElementType.TYPE}) // 可加在方法/类上
@Retention(RetentionPolicy.RUNTIME) // 运行时生效
@Documented
public @interface RequireAdmin {
    // 可扩展：比如添加message属性，默认提示语
    String message() default "仅管理员可操作";
}