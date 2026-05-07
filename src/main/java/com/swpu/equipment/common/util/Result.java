package com.swpu.equipment.common.util;

import lombok.Data;

/**
 * 全局统一返回结构体
 * @param <T> 泛型：返回数据的类型（可传User、List、String等）
 */
@Data
public class Result<T> {
    // 状态码：200成功，400参数错误，401未登录，403无权限，500服务器错误
    private Integer code;
    // 返回信息：成功/失败提示
    private String msg;
    // 返回数据：成功时返回具体数据，失败时可为null
    private T data;

    // ========== 静态工具方法（简化调用） ==========
    // 1. 成功（无数据）
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(null);
        return result;
    }

    // 2. 成功（带数据）
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }

    // 3. 失败（自定义错误信息）
    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMsg(msg);
        result.setData(null);
        return result;
    }

    // 4. 失败（自定义状态码+错误信息）
    public static <T> Result<T> error(Integer code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(null);
        return result;
    }

    // 5. 成功（带数据+刷新标识）
    public static <T> Result<T> successWithRefresh(T data, boolean needRefresh) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }
}
