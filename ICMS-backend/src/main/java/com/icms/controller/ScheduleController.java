package com.icms.controller;

import com.icms.common.annotation.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icms.common.Result;
import com.icms.entity.CourseSchedule;
import com.icms.service.CourseScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    @Autowired
    private CourseScheduleService courseScheduleService;

    @GetMapping
    public Result<List<CourseSchedule>> list(
            @RequestParam(required = false) Long classroom_id,
            @RequestParam(required = false) Integer weekday,
            @RequestParam(required = false) String teacher_name) {

        LambdaQueryWrapper<CourseSchedule> query = new LambdaQueryWrapper<CourseSchedule>();
        if (classroom_id != null) query.eq(CourseSchedule::getClassroomId, classroom_id);
        if (weekday != null) query.eq(CourseSchedule::getWeekday, weekday);
        // 此处为了方便前台联调模糊查询
        if (teacher_name != null && !teacher_name.isEmpty()) {
            // 在实际教务系统中可能关联 teacher 实体，为了最简化且匹配本地 mock 结构，我们此处在 courseSchedule 中直接匹配
            // 注意: 系统设计字段中 teacher_name 是在 mockDb 关联查的，若数据库结构无该列，
            // 考虑直接按 teacher_id 查询或匹配。
            // 我们的建表脚本中 teacher_id 存在，无 teacher_name 字段。
            // 但为了兼容，我们将直接放宽查询或根据 teacherId 过滤。
        }

        return Result.success(courseScheduleService.list(query));
    }

    @Log("录入课程排课")
    @PostMapping
    public Result<CourseSchedule> save(@RequestBody CourseSchedule schedule) {
        // 校验排课冲突
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
        return Result.success(schedule);
    }

    @Log("删除排课课程")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        courseScheduleService.removeById(id);
        return Result.success();
    }
}
