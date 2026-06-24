package com.icms.controller;

import com.icms.common.annotation.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icms.common.Result;
import com.icms.entity.Classroom;
import com.icms.service.ClassroomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/classrooms")
public class ClassroomController {

    @Autowired
    private ClassroomService classroomService;

    @GetMapping
    public Result<List<Classroom>> list(
            @RequestParam(required = false) Long campus_id,
            @RequestParam(required = false) Long building_id,
            @RequestParam(required = false) String room_type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer min_capacity,
            @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<Classroom> query = new LambdaQueryWrapper<Classroom>();
        if (campus_id != null) query.eq(Classroom::getCampusId, campus_id);
        if (building_id != null) query.eq(Classroom::getBuildingId, building_id);
        if (room_type != null && !room_type.isEmpty()) query.eq(Classroom::getRoomType, room_type);
        if (status != null && !status.isEmpty()) query.eq(Classroom::getStatus, status);
        if (min_capacity != null) query.ge(Classroom::getCapacity, min_capacity);
        if (keyword != null && !keyword.isEmpty()) {
            query.and(q -> q.like(Classroom::getName, keyword).or().like(Classroom::getRoomNo, keyword));
        }

        return Result.success(classroomService.list(query));
    }

    @Log("新增或修改教室资源")
    @PostMapping
    public Result<Classroom> save(@RequestBody Classroom classroom) {
        classroomService.saveOrUpdate(classroom);
        return Result.success(classroom);
    }

    @Log("删除教室资源")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        classroomService.removeById(id);
        return Result.success();
    }

    @GetMapping("/free")
    public Result<List<Classroom>> getFree(
            @RequestParam String date,
            @RequestParam Integer startSection,
            @RequestParam Integer endSection,
            @RequestParam(required = false) Long campus_id,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) List<String> devices) {

        LocalDate localDate = LocalDate.parse(date);
        List<Classroom> list = classroomService.getFreeClassrooms(localDate, startSection, endSection, campus_id, minCapacity, devices);
        return Result.success(list);
    }

    @GetMapping("/check-conflict")
    public Result<Map<String, Object>> checkConflict(
            @RequestParam Long classroom_id,
            @RequestParam String date,
            @RequestParam Integer startSection,
            @RequestParam Integer endSection) {

        LocalDate localDate = LocalDate.parse(date);
        boolean hasConflict = classroomService.checkConflict(classroom_id, localDate, startSection, endSection);
        
        Map<String, Object> data = new HashMap<>();
        data.put("conflict", hasConflict);
        if (hasConflict) {
            data.put("reason", "该教室在此日期时段已有安排课程或已被他人预约。");
        }
        return Result.success(data);
    }
}
