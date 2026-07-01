package com.icms.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import com.icms.common.annotation.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icms.common.BeanUtils;
import com.icms.common.Result;
import com.icms.entity.OperationLog;
import com.icms.entity.SysUser;
import com.icms.service.OperationLogService;
import com.icms.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@SaCheckLogin
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @SaCheckRole(value = {"admin", "jw"}, mode = SaMode.OR)
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String user_type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<SysUser>();
        if (user_type != null && !user_type.isEmpty()) {
            query.eq(SysUser::getUserType, user_type);
        }
        if (keyword != null && !keyword.isEmpty()) {
            query.and(q -> q.like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword));
        }

        Page<SysUser> mpPage = new Page<>(page, pageSize);
        IPage<SysUser> ipage = sysUserService.page(mpPage, query);

        List<Map<String, Object>> records = ipage.getRecords().stream()
                .map(this::userToMap)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", ipage.getTotal());
        result.put("records", records);
        return Result.success(result);
    }

    @SaCheckRole(value = {"admin", "jw"}, mode = SaMode.OR)
    @Log("创建或编辑系统账号")
    @PostMapping
    public Result<Map<String, Object>> save(@RequestBody SysUser user) {
        if (user.getId() == null) {
            String rawPwd = user.getPassword() != null ? user.getPassword() : "123456";
            user.setPassword(passwordEncoder.encode(rawPwd));
            user.setStatus(1);
            user.setCreatedAt(LocalDateTime.now());
        }
        sysUserService.saveOrUpdate(user);
        return Result.success(userToMap(user));
    }

    @Log("修改个人登录密码")
    @PostMapping("/{id}/change-pwd")
    public Result<Void> changePwd(@PathVariable Long id, @RequestBody Map<String, String> body) {
        // 只能修改本人密码；管理员改他人走 reset-pwd
        if (!id.equals(StpUtil.getLoginIdAsLong())) {
            return Result.error(403, "只能修改本人的密码");
        }
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.error("当前密码错误");
        }
        if (newPassword == null || newPassword.length() < 6) {
            return Result.error("新密码长度至少为 6 位");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        sysUserService.updateById(user);
        return Result.success();
    }

    @SaCheckRole(value = {"admin", "jw"}, mode = SaMode.OR)
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

    @SaCheckRole(value = {"admin", "jw"}, mode = SaMode.OR)
    @GetMapping("/logs")
    public Result<Map<String, Object>> getLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        LambdaQueryWrapper<OperationLog> query = new LambdaQueryWrapper<OperationLog>()
                .orderByDesc(OperationLog::getId);

        Page<OperationLog> mpPage = new Page<>(page, pageSize);
        IPage<OperationLog> ipage = operationLogService.page(mpPage, query);

        List<Map<String, Object>> records = ipage.getRecords().stream()
                .map(BeanUtils::toMap)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", ipage.getTotal());
        result.put("records", records);
        return Result.success(result);
    }

    private Map<String, Object> userToMap(SysUser user) {
        Map<String, Object> map = BeanUtils.toMap(user);
        map.remove("password");
        map.remove("deleted");
        return map;
    }
}
