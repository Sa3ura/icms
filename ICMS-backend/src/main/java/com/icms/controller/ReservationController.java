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
import com.icms.entity.ClassroomReservation;
import com.icms.entity.SysUser;
import com.icms.service.ClassroomReservationService;
import com.icms.service.ClassroomService;
import com.icms.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@SaCheckLogin
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    @Autowired
    private ClassroomReservationService classroomReservationService;

    @Autowired
    private ClassroomService classroomService;

    @Autowired
    private SysUserService sysUserService;

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) Long applicant_id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long classroom_id,
            @RequestParam(required = false) List<String> date_range,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        LambdaQueryWrapper<ClassroomReservation> query = new LambdaQueryWrapper<ClassroomReservation>();
        if (applicant_id != null) query.eq(ClassroomReservation::getApplicantId, applicant_id);
        if (status != null && !status.isEmpty()) query.eq(ClassroomReservation::getStatus, status);
        if (classroom_id != null) query.eq(ClassroomReservation::getClassroomId, classroom_id);
        if (date_range != null && date_range.size() == 2) {
            query.ge(ClassroomReservation::getReservationDate, LocalDate.parse(date_range.get(0)))
                 .le(ClassroomReservation::getReservationDate, LocalDate.parse(date_range.get(1)));
        }
        
        query.orderByDesc(ClassroomReservation::getId);
        Page<ClassroomReservation> mpPage = new Page<>(page, pageSize);
        IPage<ClassroomReservation> ipage = classroomReservationService.page(mpPage, query);
        List<Classroom> rooms = classroomService.list();
        List<SysUser> users = sysUserService.list();

        List<Map<String, Object>> records = ipage.getRecords().stream().map(r -> {
            Map<String, Object> map = BeanUtils.toMap(r);
            Classroom cr = rooms.stream().filter(c -> c.getId().equals(r.getClassroomId())).findFirst().orElse(null);
            map.put("classroom_name", cr != null ? cr.getName() : "");
            SysUser applicant = users.stream().filter(u -> u.getId().equals(r.getApplicantId())).findFirst().orElse(null);
            map.put("applicant_name", applicant != null ? applicant.getRealName() : "");
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", ipage.getTotal());
        result.put("records", records);
        return Result.success(result);
    }

    @Log("申请预约教室")
    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody ClassroomReservation reservation) {
        ClassroomReservation data = classroomReservationService.createReservation(reservation);
        return Result.success(BeanUtils.toMap(data));
    }

    @SaCheckRole(value = {"admin", "jw"}, mode = SaMode.OR)
    @Log("审批教室预约")
    @PostMapping("/{id}/approve")
    public Result<Map<String, Object>> approve(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String status = (String) body.get("status");
        String remark = (String) body.get("remark");
        Number approverIdNum = (Number) body.get("approverId");
        Long approverId = approverIdNum != null ? approverIdNum.longValue() : 1L;

        ClassroomReservation data = classroomReservationService.approveReservation(id, status, remark, approverId);
        return Result.success(BeanUtils.toMap(data));
    }

    @Log("取消教室预约")
    @PostMapping("/{id}/cancel")
    public Result<Map<String, Object>> cancel(@PathVariable Long id) {
        ClassroomReservation r = classroomReservationService.getById(id);
        if (r != null) {
            r.setStatus("cancelled");
            classroomReservationService.updateById(r);
        }
        return Result.success(BeanUtils.toMap(r));
    }
}
