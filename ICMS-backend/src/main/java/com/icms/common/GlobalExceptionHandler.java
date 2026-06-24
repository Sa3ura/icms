package com.icms.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 捕获自定义业务异常
    @ExceptionHandler(CustomException.class)
    public Result<Void> handleCustomException(CustomException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    // 捕获未登录或安全认证相关异常 (如果未被 Security 拦截器捕获)
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public Result<Void> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return Result.error(403, "权限不足，拒绝访问");
    }

    // 捕获兜底系统异常
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统崩溃异常", e);
        return Result.error(500, "服务器繁忙，请稍后再试: " + e.getMessage());
    }
}
