package com.icms.controller;

import cn.dev33.satoken.stp.SaLoginConfig;
import cn.dev33.satoken.stp.StpUtil;
import com.icms.common.annotation.Log;
import com.icms.common.BeanUtils;
import com.icms.common.Result;
import com.icms.entity.SysUser;
import com.icms.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private SysUserService sysUserService;

    @Log("用户登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        SysUser user = sysUserService.validateLogin(username, password);

        StpUtil.login(user.getId(),
                SaLoginConfig.setExtra("role", user.getUserType())
                        .setExtra("username", user.getUsername()));

        Map<String, Object> userInfo = BeanUtils.toMap(user);
        userInfo.remove("password");
        userInfo.remove("deleted");

        Map<String, Object> result = new HashMap<>();
        result.put("token", StpUtil.getTokenValue());
        result.put("userInfo", userInfo);
        return Result.success(result);
    }

    @Log("用户登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }
}
