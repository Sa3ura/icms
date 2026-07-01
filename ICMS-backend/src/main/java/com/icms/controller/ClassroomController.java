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
import com.icms.entity.Classroom;
import com.icms.service.ClassroomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "教室资源")
@SaCheckLogin
@RestController
@RequestMapping("/api/classrooms")
public class ClassroomController {

    @Autowired
    private ClassroomService classroomService;

    @Operation(summary = "分页查询教室资源")
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) Long campus_id,
            @RequestParam(required = false) Long building_id,
            @RequestParam(required = false) String room_type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer min_capacity,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        LambdaQueryWrapper<Classroom> query = new LambdaQueryWrapper<Classroom>();
        if (campus_id != null) query.eq(Classroom::getCampusId, campus_id);
        if (building_id != null) query.eq(Classroom::getBuildingId, building_id);
        if (room_type != null && !room_type.isEmpty()) query.eq(Classroom::getRoomType, room_type);
        if (status != null && !status.isEmpty()) query.eq(Classroom::getStatus, status);
        if (min_capacity != null) query.ge(Classroom::getCapacity, min_capacity);
        if (keyword != null && !keyword.isEmpty()) {
            query.and(q -> q.like(Classroom::getName, keyword).or().like(Classroom::getRoomNo, keyword));
        }

        Page<Classroom> mpPage = new Page<>(page, pageSize);
        IPage<Classroom> ipage = classroomService.page(mpPage, query);

        List<Map<String, Object>> records = ipage.getRecords().stream()
                .map(BeanUtils::toMap)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", ipage.getTotal());
        result.put("records", records);
        return Result.success(result);
    }

    @Operation(summary = "新增或修改教室资源")
    @SaCheckRole(value = {"admin", "jw"}, mode = SaMode.OR)
    @Log("新增或修改教室资源")
    @PostMapping
    public Result<Map<String, Object>> save(@RequestBody Classroom classroom) {
        classroomService.saveOrUpdate(classroom);
        return Result.success(BeanUtils.toMap(classroom));
    }

    @Operation(summary = "删除教室资源")
    @SaCheckRole(value = {"admin", "jw"}, mode = SaMode.OR)
    @Log("删除教室资源")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        classroomService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "查询空闲教室")
    @GetMapping("/free")
    public Result<List<Map<String, Object>>> getFree(
            @RequestParam String date,
            @RequestParam Integer startSection,
            @RequestParam Integer endSection,
            @RequestParam(required = false) Long campus_id,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) List<String> devices) {

        LocalDate localDate = LocalDate.parse(date);
        List<Classroom> list = classroomService.getFreeClassrooms(localDate, startSection, endSection, campus_id, minCapacity, devices);
        return Result.success(list.stream().map(BeanUtils::toMap).collect(Collectors.toList()));
    }

    @Operation(summary = "检查教室预约冲突")
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
