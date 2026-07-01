package com.icms.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.icms.common.annotation.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icms.common.BeanUtils;
import com.icms.common.Result;
import com.icms.entity.CourseSchedule;
import com.icms.service.CourseScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "排课管理")
@SaCheckLogin
@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    @Autowired
    private CourseScheduleService courseScheduleService;

    @Operation(summary = "分页查询课程排课")
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) Long classroom_id,
            @RequestParam(required = false) Integer weekday,
            @RequestParam(required = false) String teacher_name,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        LambdaQueryWrapper<CourseSchedule> query = new LambdaQueryWrapper<CourseSchedule>();
        if (classroom_id != null) query.eq(CourseSchedule::getClassroomId, classroom_id);
        if (weekday != null) query.eq(CourseSchedule::getWeekday, weekday);

        Page<CourseSchedule> mpPage = new Page<>(page, pageSize);
        IPage<CourseSchedule> ipage = courseScheduleService.page(mpPage, query);

        List<Map<String, Object>> records = ipage.getRecords().stream()
                .map(BeanUtils::toMap)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", ipage.getTotal());
        result.put("records", records);
        return Result.success(result);
    }

    @SaCheckRole(value = {"admin", "jw"}, mode = SaMode.OR)
    @Log("录入课程排课")
    @Operation(summary = "录入或修改课程排课")
    @PostMapping
    public Result<Map<String, Object>> save(@RequestBody CourseSchedule schedule) {
        Long classroomId = schedule.getClassroomId();
        Integer weekday = schedule.getWeekday();
        Integer start = schedule.getStartSection();
        Integer end = schedule.getEndSection();
        
        LambdaQueryWrapper<CourseSchedule> conflictQuery = new LambdaQueryWrapper<CourseSchedule>()
                .eq(CourseSchedule::getClassroomId, classroomId)
                .eq(CourseSchedule::getWeekday, weekday)
                .le(CourseSchedule::getStartSection, end)
                .ge(CourseSchedule::getEndSection, start);
                
        if (schedule.getId() != null) {
            conflictQuery.ne(CourseSchedule::getId, schedule.getId());
        }

        long count = courseScheduleService.count(conflictQuery);
        if (count > 0) {
            return Result.error("排课冲突：该教室在此时间段已有排课课程");
        }

        if (schedule.getId() == null) {
            schedule.setStatus("published");
            schedule.setCreatedAt(LocalDateTime.now());
        }
        courseScheduleService.saveOrUpdate(schedule);
        return Result.success(BeanUtils.toMap(schedule));
    }

    @SaCheckRole(value = {"admin", "jw"}, mode = SaMode.OR)
    @Log("删除排课课程")
    @Operation(summary = "删除排课记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        courseScheduleService.removeById(id);
        return Result.success();
    }
}
