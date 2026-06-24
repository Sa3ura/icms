package com.icms.controller;

import cn.dev33.satoken.stp.SaLoginConfig;
import cn.dev33.satoken.stp.StpUtil;
import com.icms.common.BeanUtils;
import com.icms.common.CustomException;
import com.icms.common.Result;
import com.icms.common.annotation.Log;
import com.icms.entity.SysUser;
import com.icms.service.SysUserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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

    @Log("用户登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> request,
                                              HttpServletResponse response) {
        String username = request.get("username");
        String password = request.get("password");

        SysUser user = sysUserService.validateLogin(username, password);

        StpUtil.login(user.getId(),
                SaLoginConfig.setExtra("role", user.getUserType())
                        .setExtra("username", user.getUsername()));

        String refreshToken = sysUserService.rotateRefreshToken(user.getId());

        Cookie cookie = new Cookie(rfCookieName, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // 本地开发不用 HTTPS，生产环境改为 true
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

    @Log("用户登出")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletResponse response) {
        if (StpUtil.isLogin()) {
            sysUserService.clearRefreshToken(StpUtil.getLoginIdAsLong());
        }
        StpUtil.logout();

        Cookie cookie = new Cookie(rfCookieName, "");
        cookie.setHttpOnly(true);
        cookie.setPath(rfPath);
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return Result.success();
    }

    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh(HttpServletRequest request,
                                                HttpServletResponse response) {
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
