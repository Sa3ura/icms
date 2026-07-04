package com.icms.common.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icms.common.annotation.Log;
import com.icms.common.util.RequestClientUtil;
import com.icms.entity.OperationLog;
import com.icms.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
public class LogAspect {

    private static final String ANONYMOUS = "Anonymous";

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private ObjectMapper objectMapper;

    @Around("@annotation(controllerLog)")
    public Object doAround(ProceedingJoinPoint joinPoint, Log controllerLog) throws Throwable {
        OperationLog opLog = new OperationLog();
        opLog.setOperation(controllerLog.value());
        opLog.setCreatedAt(LocalDateTime.now());

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            opLog.setRequestUrl(request.getRequestURI());
            opLog.setRequestMethod(request.getMethod());
            opLog.setRequestIp(RequestClientUtil.getClientIp(request));
        }

        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                opLog.setRequestParams(objectMapper.writeValueAsString(args[0]));
            }
        } catch (Exception e) {
            opLog.setRequestParams("[Serialization Failed]");
        }

        populateUser(opLog);

        Object result;
        try {
            result = joinPoint.proceed();
            opLog.setResultStatus("success");
            populateUserIfAnonymous(opLog);
        } catch (Throwable e) {
            opLog.setResultStatus("fail");
            opLog.setErrorMessage(e.getMessage());
            populateUserIfAnonymous(opLog);
            throw e;
        } finally {
            try {
                operationLogService.save(opLog);
            } catch (Exception e) {
                log.error("Failed to save operation log", e);
            }
        }

        return result;
    }

    private void populateUserIfAnonymous(OperationLog opLog) {
        if (opLog.getUserId() == null || ANONYMOUS.equals(opLog.getUsername())) {
            populateUser(opLog);
        }
    }

    private void populateUser(OperationLog opLog) {
        try {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            if (loginId != null) {
                Object username = StpUtil.getExtra("username");
                opLog.setUsername(username != null ? username.toString() : loginId.toString());
                opLog.setUserId(Long.valueOf(loginId.toString()));
            } else if (opLog.getUsername() == null) {
                opLog.setUsername(ANONYMOUS);
            }
        } catch (Exception e) {
            if (opLog.getUsername() == null) {
                opLog.setUsername(ANONYMOUS);
            }
        }
    }
}
