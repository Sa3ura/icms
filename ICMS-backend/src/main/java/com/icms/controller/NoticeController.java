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
import com.icms.service.NoticeService;
import com.icms.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@SaCheckLogin
@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private SysUserService sysUserService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

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

    @SaCheckRole(value = {"admin", "jw"}, mode = SaMode.OR)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.removeById(id);
        return Result.success();
    }

    @PutMapping("/{id}/read")
    public Result<Boolean> markRead(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Notice notice = noticeService.getById(id);
        if (notice == null) {
            return Result.error("通知不存在");
        }
        // 已读用户取当前登录者，忽略请求体传入，防止代他人标记
        Long userId = StpUtil.getLoginIdAsLong();
        try {
            List<Long> readBy = objectMapper.readValue(
                    notice.getReadBy() != null ? notice.getReadBy() : "[]",
                    new TypeReference<List<Long>>() {});
            if (!readBy.contains(userId)) {
                readBy.add(userId);
                notice.setReadBy(objectMapper.writeValueAsString(readBy));
                noticeService.updateById(notice);
            }
        } catch (Exception e) {
            return Result.error("已读标记处理失败");
        }
        return Result.success(true);
    }

    @PutMapping("/read-all")
    public Result<Map<String, Object>> markAllRead(@RequestBody(required = false) Map<String, String> body) {
        String role = body != null ? body.get("role") : null;
        // 已读用户取当前登录者，忽略请求体传入
        Long userId = StpUtil.getLoginIdAsLong();

        List<Notice> all = noticeService.list();
        int marked = 0;
        try {
            for (Notice notice : all) {
                if (notice.getTargetRole() != null && !notice.getTargetRole().isEmpty()
                        && !notice.getTargetRole().equals(role)) {
                    continue;
                }
                List<Long> readBy = objectMapper.readValue(
                        notice.getReadBy() != null ? notice.getReadBy() : "[]",
                        new TypeReference<List<Long>>() {});
                if (!readBy.contains(userId)) {
                    readBy.add(userId);
                    notice.setReadBy(objectMapper.writeValueAsString(readBy));
                    noticeService.updateById(notice);
                    marked++;
                }
            }
        } catch (Exception e) {
            return Result.error("全部已读操作失败");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("marked", marked);
        return Result.success(result);
    }

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
