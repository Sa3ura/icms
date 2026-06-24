package com.icms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icms.common.Result;
import com.icms.common.BeanUtils;
import com.icms.entity.Notice;
import com.icms.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        List<Notice> notices = noticeService.list(new LambdaQueryWrapper<Notice>().orderByDesc(Notice::getId));
        List<Map<String, Object>> result = notices.stream().map(n -> {
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
        return Result.success(result);
    }

    @PostMapping
    public Result<Map<String, Object>> save(@RequestBody Notice notice) {
        if (notice.getId() == null) {
            notice.setPublisherId(1L);
            notice.setStatus("published");
            notice.setReadBy("[]");
            notice.setCreatedAt(LocalDateTime.now());
        }
        noticeService.saveOrUpdate(notice);
        return Result.success(BeanUtils.toMap(notice));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.removeById(id);
        return Result.success();
    }

    @PutMapping("/{id}/read")
    public Result<Boolean> markRead(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Notice notice = noticeService.getById(id);
        if (notice == null) {
            return Result.error("通知不存在");
        }
        Long userId = Long.valueOf(body.get("userId").toString());
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
    public Result<Map<String, Object>> markAllRead(@RequestBody Map<String, String> body) {
        String role = body.get("role");
        Long userId = Long.valueOf(body.get("userId"));

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
}
