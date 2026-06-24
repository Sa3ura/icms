package com.icms.common.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icms.common.annotation.Log;
import com.icms.entity.OperationLog;
import com.icms.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
public class LogAspect {

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private ObjectMapper objectMapper;

    @Around("@annotation(controllerLog)")
    public Object doAround(ProceedingJoinPoint joinPoint, Log controllerLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        OperationLog opLog = new OperationLog();
        opLog.setOperation(controllerLog.value());
        opLog.setCreatedAt(LocalDateTime.now());

        // 获取 Request
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            opLog.setRequestUrl(request.getRequestURI());
            opLog.setRequestMethod(request.getMethod());
            opLog.setRequestIp(request.getRemoteAddr());
        }

        // 获取参数
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                // 仅序列化非 Request/Response 等系统参数
                opLog.setRequestParams(objectMapper.writeValueAsString(args[0]));
            }
        } catch (Exception e) {
            opLog.setRequestParams("[Serialization Failed]");
        }

        // 获取用户信息
        try {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            if (loginId != null) {
                opLog.setUsername((String) StpUtil.getExtra("username"));
                opLog.setUserId(Long.valueOf(loginId.toString()));
            } else {
                opLog.setUsername("Anonymous");
            }
        } catch (Exception e) {
            opLog.setUsername("Anonymous");
        }

        Object result;
        try {
            result = joinPoint.proceed();
            opLog.setResultStatus("success");
        } catch (Throwable e) {
            opLog.setResultStatus("fail");
            opLog.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            try {
                // 异步存入数据库
                operationLogService.save(opLog);
            } catch (Exception e) {
                log.error("Failed to save operation log", e);
            }
        }

        return result;
    }
}
