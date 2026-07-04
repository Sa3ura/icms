package com.icms.common.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icms.common.annotation.Log;
import com.icms.entity.OperationLog;
import com.icms.service.OperationLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogAspectTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void fillsUserAfterSuccessfulLoginLikeOperation() throws Throwable {
        OperationLogService operationLogService = mock(OperationLogService.class);
        LogAspect aspect = newAspect(operationLogService);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{Map.of("username", "admin")});
        when(joinPoint.proceed()).thenAnswer(invocation -> null);
        setRequest("POST", "/api/auth/login", "203.0.113.10");

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdDefaultNull).thenReturn(null, 1L);
            stp.when(() -> StpUtil.getExtra("username")).thenReturn("admin");

            aspect.doAround(joinPoint, logAnnotation());
        }

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogService).save(captor.capture());
        OperationLog saved = captor.getValue();
        assertEquals(1L, saved.getUserId());
        assertEquals("admin", saved.getUsername());
        assertEquals("203.0.113.10", saved.getRequestIp());
        assertEquals("success", saved.getResultStatus());
    }

    @Test
    void keepsPreLogoutUserWhenOperationClearsLoginState() throws Throwable {
        OperationLogService operationLogService = mock(OperationLogService.class);
        LogAspect aspect = newAspect(operationLogService);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenAnswer(invocation -> null);
        setRequest("POST", "/api/auth/logout", "198.51.100.10");

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdDefaultNull).thenReturn(1L).thenReturn(null);
            stp.when(() -> StpUtil.getExtra("username")).thenReturn("admin");

            aspect.doAround(joinPoint, logAnnotation());
        }

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogService).save(captor.capture());
        OperationLog saved = captor.getValue();
        assertEquals(1L, saved.getUserId());
        assertEquals("admin", saved.getUsername());
    }

    @Test
    void failedAnonymousOperationStaysAnonymous() throws Throwable {
        OperationLogService operationLogService = mock(OperationLogService.class);
        LogAspect aspect = newAspect(operationLogService);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));
        setRequest("POST", "/api/auth/login", "198.51.100.20");

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdDefaultNull).thenReturn(null);

            assertThrows(IllegalStateException.class, () -> aspect.doAround(joinPoint, logAnnotation()));
        }

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogService).save(captor.capture());
        OperationLog saved = captor.getValue();
        assertEquals("Anonymous", saved.getUsername());
        assertEquals("fail", saved.getResultStatus());
        assertEquals("boom", saved.getErrorMessage());
    }

    private LogAspect newAspect(OperationLogService operationLogService) {
        LogAspect aspect = new LogAspect();
        ReflectionTestUtils.setField(aspect, "operationLogService", operationLogService);
        ReflectionTestUtils.setField(aspect, "objectMapper", new ObjectMapper());
        return aspect;
    }

    private void setRequest(String method, String uri, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", forwardedFor + ", 127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private Log logAnnotation() throws NoSuchMethodException {
        Method method = AnnotatedOperations.class.getDeclaredMethod("operation");
        return method.getAnnotation(Log.class);
    }

    private static class AnnotatedOperations {
        @Log("test operation")
        void operation() {
        }
    }
}
