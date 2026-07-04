package com.icms.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.icms.common.CustomException;
import com.icms.entity.SysUser;
import com.icms.security.CaptchaUtil;
import com.icms.security.LoginLimiter;
import com.icms.service.SysUserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @Test
    void loginSucceedsWithValidCaptchaAndCredentials() {
        SysUserService sysUserService = mock(SysUserService.class);
        AuthController controller = newAuthController(sysUserService);

        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setUserType("admin");
        user.setPassword("hashed");
        user.setRealName("管理员");
        user.setRefreshToken("rt-hash");
        user.setDeleted(0);

        when(sysUserService.validateLogin("admin", "123456")).thenReturn(user);
        when(sysUserService.rotateRefreshToken(1L)).thenReturn("new-refresh-token");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        HttpServletResponse response = mock(HttpServletResponse.class);

        Map<String, String> body = new HashMap<>();
        body.put("username", "admin");
        body.put("password", "123456");
        body.put("captchaId", "abc123");
        body.put("captchaCode", "ABCD");

        try (MockedStatic<CaptchaUtil> captcha = mockStatic(CaptchaUtil.class);
             MockedStatic<LoginLimiter> limiter = mockStatic(LoginLimiter.class);
             MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {

            captcha.when(() -> CaptchaUtil.validate("abc123", "ABCD")).thenReturn(true);
            String rateKey = "127.0.0.1::admin";
            limiter.when(() -> LoginLimiter.buildKey("admin", "127.0.0.1")).thenReturn(rateKey);
            limiter.when(() -> LoginLimiter.isBlocked(rateKey)).thenReturn(false);
            stp.when(StpUtil::getTokenValue).thenReturn("access-token");

            var result = controller.login(body, request, response);

            assertEquals(200, result.getCode());
            assertNotNull(result.getData());
            assertEquals("access-token", result.getData().get("token"));

            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = (Map<String, Object>) result.getData().get("userInfo");
            assertNotNull(userInfo);
            assertFalse(userInfo.containsKey("password"));
            assertFalse(userInfo.containsKey("refresh_token"));
            assertFalse(userInfo.containsKey("deleted"));

            limiter.verify(() -> LoginLimiter.clear(rateKey));
            verify(response).addCookie(any(Cookie.class));
        }
    }

    @Test
    void loginRateLimitUsesForwardedClientIp() {
        SysUserService sysUserService = mock(SysUserService.class);
        AuthController controller = newAuthController(sysUserService);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
        when(request.getHeader("True-Client-IP")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.60, 127.0.0.1");
        when(request.getHeader("Forwarded")).thenReturn(null);

        Map<String, String> body = new HashMap<>();
        body.put("username", "admin");
        body.put("password", "123456");
        body.put("captchaId", "x");
        body.put("captchaCode", "y");

        try (MockedStatic<LoginLimiter> limiter = mockStatic(LoginLimiter.class)) {
            String rateKey = "198.51.100.60::admin";
            limiter.when(() -> LoginLimiter.buildKey("admin", "198.51.100.60")).thenReturn(rateKey);
            limiter.when(() -> LoginLimiter.isBlocked(rateKey)).thenReturn(true);
            limiter.when(() -> LoginLimiter.getBlockSeconds(rateKey)).thenReturn(900L);

            assertThrows(CustomException.class, () -> controller.login(body, request, mock(HttpServletResponse.class)));

            limiter.verify(() -> LoginLimiter.buildKey("admin", "198.51.100.60"));
            verify(sysUserService, never()).validateLogin(any(), any());
        }
    }

    @Test
    void loginFailsWithInvalidCaptcha() {
        SysUserService sysUserService = mock(SysUserService.class);
        AuthController controller = newAuthController(sysUserService);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        Map<String, String> body = new HashMap<>();
        body.put("username", "admin");
        body.put("password", "123456");
        body.put("captchaId", "bad");
        body.put("captchaCode", "WRONG");

        try (MockedStatic<CaptchaUtil> captcha = mockStatic(CaptchaUtil.class);
             MockedStatic<LoginLimiter> limiter = mockStatic(LoginLimiter.class)) {

            captcha.when(() -> CaptchaUtil.validate("bad", "WRONG")).thenReturn(false);
            String rateKey = "192.168.1.1::admin";
            limiter.when(() -> LoginLimiter.buildKey("admin", "192.168.1.1")).thenReturn(rateKey);
            limiter.when(() -> LoginLimiter.isBlocked(rateKey)).thenReturn(false);

            CustomException ex = assertThrows(CustomException.class, () -> controller.login(body, request, mock(HttpServletResponse.class)));
            assertTrue(ex.getMessage().contains("验证码"));

            limiter.verify(() -> LoginLimiter.recordFailure(rateKey));
            verify(sysUserService, never()).validateLogin(any(), any());
        }
    }

    @Test
    void loginFailsWhenRateLimited() {
        SysUserService sysUserService = mock(SysUserService.class);
        AuthController controller = newAuthController(sysUserService);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        Map<String, String> body = new HashMap<>();
        body.put("username", "admin");
        body.put("password", "123456");
        body.put("captchaId", "x");
        body.put("captchaCode", "y");

        try (MockedStatic<LoginLimiter> limiter = mockStatic(LoginLimiter.class)) {
            String rateKey = "10.0.0.1::admin";
            limiter.when(() -> LoginLimiter.buildKey("admin", "10.0.0.1")).thenReturn(rateKey);
            limiter.when(() -> LoginLimiter.isBlocked(rateKey)).thenReturn(true);
            limiter.when(() -> LoginLimiter.getBlockSeconds(rateKey)).thenReturn(900L);

            CustomException ex = assertThrows(CustomException.class, () -> controller.login(body, request, mock(HttpServletResponse.class)));
            assertTrue(ex.getMessage().contains("频繁"));

            verify(sysUserService, never()).validateLogin(any(), any());
        }
    }

    @Test
    void loginFailureRecordsRetryOnBadCredentials() {
        SysUserService sysUserService = mock(SysUserService.class);
        AuthController controller = newAuthController(sysUserService);

        when(sysUserService.validateLogin("admin", "wrong"))
                .thenThrow(new CustomException("用户名或密码错误"));

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("172.16.0.1");

        Map<String, String> body = new HashMap<>();
        body.put("username", "admin");
        body.put("password", "wrong");
        body.put("captchaId", "good");
        body.put("captchaCode", "ABCD");

        try (MockedStatic<CaptchaUtil> captcha = mockStatic(CaptchaUtil.class);
             MockedStatic<LoginLimiter> limiter = mockStatic(LoginLimiter.class)) {

            captcha.when(() -> CaptchaUtil.validate("good", "ABCD")).thenReturn(true);
            String rateKey = "172.16.0.1::admin";
            limiter.when(() -> LoginLimiter.buildKey("admin", "172.16.0.1")).thenReturn(rateKey);
            limiter.when(() -> LoginLimiter.isBlocked(rateKey)).thenReturn(false);

            assertThrows(CustomException.class, () -> controller.login(body, request, mock(HttpServletResponse.class)));

            limiter.verify(() -> LoginLimiter.recordFailure(rateKey));
            limiter.verify(() -> LoginLimiter.clear(rateKey), never());
        }
    }

    @Test
    void captchaReturnsSvgWithCaptchaId() {
        try (MockedStatic<CaptchaUtil> captcha = mockStatic(CaptchaUtil.class)) {
            captcha.when(() -> CaptchaUtil.newCaptchaId()).thenReturn("test-id");
            captcha.when(() -> CaptchaUtil.generateSVG("test-id")).thenReturn("<svg>dummy</svg>");

            AuthController controller = new AuthController();
            String result = controller.captcha();

            assertNotNull(result);
            assertTrue(result.contains("test-id"));
        }
    }

    private AuthController newAuthController(SysUserService sysUserService) {
        AuthController controller = new AuthController();
        ReflectionTestUtils.setField(controller, "sysUserService", sysUserService);
        ReflectionTestUtils.setField(controller, "rfCookieName", "icms_rf");
        ReflectionTestUtils.setField(controller, "rfPath", "/api");
        ReflectionTestUtils.setField(controller, "rfMaxAge", 604800);
        ReflectionTestUtils.setField(controller, "rfSecure", false);
        ReflectionTestUtils.setField(controller, "captchaEnabled", true);
        return controller;
    }
}
