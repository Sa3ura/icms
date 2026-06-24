package com.icms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icms.common.Result;
import com.icms.entity.Notice;
import com.icms.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    @GetMapping
    public Result<List<Notice>> list() {
        LambdaQueryWrapper<Notice> query = new LambdaQueryWrapper<Notice>()
                .orderByDesc(Notice::getId);
        return Result.success(noticeService.list(query));
    }

    @PostMapping
    public Result<Notice> save(@RequestBody Notice notice) {
        if (notice.getId() == null) {
            notice.setPublisherId(1L);
            notice.setStatus("published");
            notice.setCreatedAt(LocalDateTime.now());
        }
        noticeService.saveOrUpdate(notice);
        return Result.success(notice);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.removeById(id);
        return Result.success();
    }
}
