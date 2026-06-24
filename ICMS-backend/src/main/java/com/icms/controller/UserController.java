package com.icms.controller;

import com.icms.common.annotation.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icms.common.Result;
import com.icms.entity.OperationLog;
import com.icms.entity.SysUser;
import com.icms.service.OperationLogService;
import com.icms.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public Result<List<SysUser>> list(
            @RequestParam(required = false) String user_type,
            @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<SysUser>();
        if (user_type != null && !user_type.isEmpty()) {
            query.eq(SysUser::getUserType, user_type);
        }
        if (keyword != null && !keyword.isEmpty()) {
            query.and(q -> q.like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword));
        }

        return Result.success(sysUserService.list(query));
    }

    @Log("创建或编辑系统账号")
    @PostMapping
    public Result<SysUser> save(@RequestBody SysUser user) {
        if (user.getId() == null) {
            // 新增时，如果有传入密码就用传入的，否则默认为 123456
            String rawPwd = user.getPassword() != null ? user.getPassword() : "123456";
            user.setPassword(passwordEncoder.encode(rawPwd));
            user.setStatus(1);
            user.setCreatedAt(LocalDateTime.now());
        }
        sysUserService.saveOrUpdate(user);
        return Result.success(user);
    }

    @Log("重置用户密码")
    @PostMapping("/{id}/reset-pwd")
    public Result<Void> resetPwd(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.length() < 6) {
            return Result.error("密码长度至少为 6 位");
        }

        SysUser user = sysUserService.getById(id);
        if (user != null) {
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setUpdatedAt(LocalDateTime.now());
            sysUserService.updateById(user);
        }
        return Result.success();
    }

    @GetMapping("/logs")
    public Result<List<OperationLog>> getLogs() {
        LambdaQueryWrapper<OperationLog> query = new LambdaQueryWrapper<OperationLog>()
                .orderByDesc(OperationLog::getId);
        return Result.success(operationLogService.list(query));
    }
}
