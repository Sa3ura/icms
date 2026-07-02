package com.icms.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icms.common.Result;
import com.icms.common.BeanUtils;
import com.icms.entity.Notice;
import com.icms.entity.SysUser;
import com.icms.mapper.NoticeMapper;
import com.icms.service.NoticeService;
import com.icms.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "通知公告")
@SaCheckLogin
@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private NoticeMapper noticeMapper;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Operation(summary = "分页查询通知公告")
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Notice> mpPage = new Page<>(page, pageSize);
        IPage<Notice> ipage = noticeService.page(mpPage,
                new LambdaQueryWrapper<Notice>().orderByDesc(Notice::getId));

        List<Map<String, Object>> records = ipage.getRecords().stream().map(n -> {
            Map<String, Object> m = BeanUtils.toMap(n);
            try {
                List<Long> readBy = objectMapper.readValue(
                        n.getReadBy() != null ? n.getReadBy() : "[]",
                        new TypeReference<List<Long>>() {});
                m.put("read_count", readBy.size());
            } catch (Exception e) {
                m.put("read_count", 0);
            }
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", ipage.getTotal());
        result.put("records", records);
        return Result.success(result);
    }

    @Operation(summary = "发布或编辑通知公告")
    @SaCheckRole(value = {"admin", "jw"}, mode = SaMode.OR)
    @PostMapping
    public Result<Map<String, Object>> save(@RequestBody Notice notice) {
        if (notice.getId() == null) {
            // 发布人取当前登录用户，替换原来硬编码的 1L
            notice.setPublisherId(StpUtil.getLoginIdAsLong());
            notice.setStatus("published");
            notice.setReadBy("[]");
            notice.setCreatedAt(LocalDateTime.now());
        }
        noticeService.saveOrUpdate(notice);
        return Result.success(BeanUtils.toMap(notice));
    }

    @Operation(summary = "删除通知公告")
    @SaCheckRole(value = {"admin", "jw"}, mode = SaMode.OR)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "标记单条通知为已读")
    @PutMapping("/{id}/read")
    public Result<Boolean> markRead(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        // 已读用户取当前登录者，忽略请求体传入，防止代他人标记
        Long userId = StpUtil.getLoginIdAsLong();
        // 原子追加：DB 层单条 SQL 处理"若未读则追加"，消除并发覆盖竞态
        int rows = noticeMapper.markReadAtomic(id, userId);
        if (rows == 0) {
            // 0 行：可能已读（幂等成功）或通知不存在（应报错）。查一次以保留原有错误语义
            if (noticeService.getById(id) == null) {
                return Result.error("通知不存在");
            }
        }
        return Result.success(true);
    }

    @Operation(summary = "一键全部已读")
    @PutMapping("/read-all")
    public Result<Map<String, Object>> markAllRead(@RequestBody(required = false) Map<String, String> body) {
        String role = body != null ? body.get("role") : null;
        // 已读用户取当前登录者，忽略请求体传入
        Long userId = StpUtil.getLoginIdAsLong();
        // 一条批量 UPDATE 完成：目标角色匹配 + 未读过滤 + 原子追加。不再"全表 list → 内存循环 update"
        int marked = noticeMapper.markAllReadAtomic(userId, role);
        Map<String, Object> result = new HashMap<>();
        result.put("marked", marked);
        return Result.success(result);
    }

    @Operation(summary = "获取通知已读用户列表")
    @SaCheckRole(value = {"admin", "jw"}, mode = SaMode.OR)
    @GetMapping("/{id}/read-users")
    public Result<List<Map<String, Object>>> getReadUsers(@PathVariable Long id) {
        Notice notice = noticeService.getById(id);
        if (notice == null) return Result.error("通知不存在");
        try {
            List<Long> readBy = objectMapper.readValue(
                    notice.getReadBy() != null ? notice.getReadBy() : "[]",
                    new TypeReference<List<Long>>() {});
            List<Map<String, Object>> users = new ArrayList<>();
            for (Long uid : readBy) {
                SysUser u = sysUserService.getById(uid);
                if (u != null) {
                    Map<String, Object> mu = new HashMap<>();
                    mu.put("id", u.getId());
                    mu.put("real_name", u.getRealName());
                    mu.put("user_type", u.getUserType());
                    users.add(mu);
                }
            }
            return Result.success(users);
        } catch (Exception e) {
            return Result.error("读取已读用户失败");
        }
    }
}
