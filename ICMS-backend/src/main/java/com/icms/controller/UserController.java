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
import com.icms.common.CustomException;
import com.icms.common.Result;
import com.icms.entity.OperationLog;
import com.icms.entity.SysUser;
import com.icms.service.OperationLogService;
import com.icms.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "用户与系统")
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

    @Operation(summary = "分页查询系统用户")
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
    @Operation(summary = "创建或编辑系统账号")
    @PostMapping
    public Result<Map<String, Object>> save(@RequestBody Map<String, Object> body) {
        Long id = longValue(body.get("id"));
        SysUser user = id == null ? new SysUser() : sysUserService.getById(id);
        if (user == null) {
            throw new CustomException("User not found");
        }

        applyUserFields(user, body);

        if (id == null) {
            String rawPwd = stringValue(body.get("password"));
            if (rawPwd == null || rawPwd.isEmpty()) {
                rawPwd = "123456";
            }
            user.setPassword(passwordEncoder.encode(rawPwd));
            user.setStatus(1);
            user.setCreatedAt(LocalDateTime.now());
        } else {
            user.setUpdatedAt(LocalDateTime.now());
        }
        sysUserService.saveOrUpdate(user);
        return Result.success(userToMap(user));
    }

    @SaCheckRole(value = {"admin", "jw"}, mode = SaMode.OR)
    @Log("删除系统账号")
    @Operation(summary = "删除系统账号")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            throw new CustomException("User not found");
        }
        if ("admin".equals(user.getUsername())) {
            throw new CustomException("Admin user cannot be deleted");
        }
        sysUserService.removeById(id);
        return Result.success();
    }

    @Log("修改个人登录密码")
    @Operation(summary = "修改个人登录密码")
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
    @Operation(summary = "重置用户密码")
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

    @Operation(summary = "分页查询操作日志")
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
        map.remove("refresh_token");
        map.remove("deleted");
        return map;
    }

    private void applyUserFields(SysUser user, Map<String, Object> body) {
        if (body.containsKey("username")) user.setUsername(stringValue(body.get("username")));
        if (body.containsKey("realName")) user.setRealName(stringValue(body.get("realName")));
        if (body.containsKey("real_name")) user.setRealName(stringValue(body.get("real_name")));
        if (body.containsKey("email")) user.setEmail(stringValue(body.get("email")));
        if (body.containsKey("phone")) user.setPhone(stringValue(body.get("phone")));
        if (body.containsKey("userType")) user.setUserType(stringValue(body.get("userType")));
        if (body.containsKey("user_type")) user.setUserType(stringValue(body.get("user_type")));
        if (body.containsKey("status")) user.setStatus(intValue(body.get("status")));
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        String text = String.valueOf(value);
        return text.isEmpty() ? null : Long.parseLong(text);
    }

    private Integer intValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        String text = String.valueOf(value);
        return text.isEmpty() ? null : Integer.parseInt(text);
    }
}
