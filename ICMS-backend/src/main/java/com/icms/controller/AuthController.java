package com.icms.controller;

import cn.dev33.satoken.stp.SaLoginConfig;
import cn.dev33.satoken.stp.StpUtil;
import com.icms.common.BeanUtils;
import com.icms.common.CustomException;
import com.icms.common.Result;
import com.icms.common.annotation.Log;
import com.icms.common.util.RequestClientUtil;
import com.icms.entity.SysUser;
import com.icms.security.CaptchaUtil;
import com.icms.security.LoginLimiter;
import com.icms.service.SysUserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private SysUserService sysUserService;

    @Value("${icms.refresh-token.cookie-name:icms_rf}")
    private String rfCookieName;

    @Value("${icms.refresh-token.max-age:604800}")
    private int rfMaxAge;

    @Value("${icms.refresh-token.path:/api}")
    private String rfPath;

    @Value("${icms.refresh-token.secure:false}")
    private boolean rfSecure;

    @Value("${icms.captcha.enabled:true}")
    private boolean captchaEnabled;

    @Operation(summary = "获取登录验证码SVG")
    @GetMapping(value = "/captcha", produces = "image/svg+xml")
    public String captcha() {
        String captchaId = CaptchaUtil.newCaptchaId();
        String svg = CaptchaUtil.generateSVG(captchaId);
        return "<!--" + captchaId + "-->\n" + svg;
    }

    @Operation(summary = "用户登录（账号密码+验证码）")
    @Log("用户登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> request,
                                              @Parameter(hidden = true) HttpServletRequest httpRequest,
                                              @Parameter(hidden = true) HttpServletResponse response) {
        String username = request.get("username");
        String password = request.get("password");
        String captchaId = request.get("captchaId");
        String captchaCode = request.get("captchaCode");

        String ip = RequestClientUtil.getClientIp(httpRequest);
        String rateKey = LoginLimiter.buildKey(username, ip);

        // 1. 频率限制检查
        if (LoginLimiter.isBlocked(rateKey)) {
            long sec = LoginLimiter.getBlockSeconds(rateKey);
            throw new CustomException("登录尝试过于频繁，请 " + sec + " 秒后再试");
        }

        // 2. 验证码校验
        if (captchaEnabled && (captchaId == null || captchaCode == null || !CaptchaUtil.validate(captchaId, captchaCode))) {
            LoginLimiter.recordFailure(rateKey);
            throw new CustomException("验证码错误或已过期，请刷新重试");
        }

        SysUser user;
        try {
            user = sysUserService.validateLogin(username, password);
        } catch (CustomException e) {
            LoginLimiter.recordFailure(rateKey);
            throw e;
        }

        LoginLimiter.clear(rateKey);

        StpUtil.login(user.getId(),
                SaLoginConfig.setExtra("role", user.getUserType())
                        .setExtra("username", user.getUsername()));

        String refreshToken = sysUserService.rotateRefreshToken(user.getId());

        Cookie cookie = new Cookie(rfCookieName, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(rfSecure);
        cookie.setPath(rfPath);
        cookie.setMaxAge(rfMaxAge);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);

        Map<String, Object> userInfo = BeanUtils.toMap(user);
        userInfo.remove("password");
        userInfo.remove("deleted");
        userInfo.remove("refresh_token");

        Map<String, Object> result = new HashMap<>();
        result.put("token", StpUtil.getTokenValue());
        result.put("userInfo", userInfo);
        return Result.success(result);
    }

    @Operation(summary = "退出登录")
    @Log("用户登出")
    @PostMapping("/logout")
    public Result<Void> logout(@Parameter(hidden = true) HttpServletResponse response) {
        if (StpUtil.isLogin()) {
            sysUserService.clearRefreshToken(StpUtil.getLoginIdAsLong());
        }
        StpUtil.logout();

        Cookie cookie = new Cookie(rfCookieName, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(rfSecure);
        cookie.setPath(rfPath);
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);

        return Result.success();
    }

    @Operation(summary = "���¿来访令牌")
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh(@Parameter(hidden = true) HttpServletRequest request,
                                                @Parameter(hidden = true) HttpServletResponse response) {
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (rfCookieName.equals(c.getName())) {
                    refreshToken = c.getValue();
                    break;
                }
            }
        }
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new CustomException(401, "刷新令牌不存在");
        }

        SysUser user = sysUserService.findByRefreshToken(refreshToken);
        if (user == null || user.getStatus() != 1) {
            throw new CustomException(401, "刷新令牌无效或账号已禁用");
        }

        StpUtil.login(user.getId(),
                SaLoginConfig.setExtra("role", user.getUserType())
                        .setExtra("username", user.getUsername()));

        String newRefreshToken = sysUserService.rotateRefreshToken(user.getId());

        Cookie cookie = new Cookie(rfCookieName, newRefreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(rfSecure);
        cookie.setPath(rfPath);
        cookie.setMaxAge(rfMaxAge);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);

        Map<String, Object> userInfo = BeanUtils.toMap(user);
        userInfo.remove("password");
        userInfo.remove("deleted");
        userInfo.remove("refresh_token");

        Map<String, Object> result = new HashMap<>();
        result.put("token", StpUtil.getTokenValue());
        result.put("userInfo", userInfo);
        return Result.success(result);
    }
}
