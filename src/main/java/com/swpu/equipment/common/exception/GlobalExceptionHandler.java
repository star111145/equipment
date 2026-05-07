package com.swpu.equipment.common.exception;

import com.swpu.equipment.common.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
// 全局异常处理（业务异常、系统异常，接口异常）
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.error("业务异常：请求URL={}, 异常信息={}", request.getRequestURI(), e.getMessage());
        return Result.error(401, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常：请求URL={}, 异常信息={}", request.getRequestURI(), e.getMessage(), e);
        return Result.error("系统异常，请稍后重试");
    }
}
