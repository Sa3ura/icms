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
import com.icms.entity.Classroom;
import com.icms.entity.ClassroomReservation;
import com.icms.entity.SysUser;
import com.icms.service.ClassroomReservationService;
import com.icms.service.ClassroomService;
import com.icms.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "预约管理")
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

    @Operation(summary = "分页查询教室预约")
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

        // 只按当前页出现过的 id 反查字典，不再全表拉 rooms / users
        List<ClassroomReservation> pageRecords = ipage.getRecords();
        Set<Long> roomIds = pageRecords.stream().map(ClassroomReservation::getClassroomId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> userIds = pageRecords.stream().map(ClassroomReservation::getApplicantId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> roomNames = roomIds.isEmpty() ? Collections.emptyMap()
                : classroomService.listByIds(roomIds).stream()
                        .collect(Collectors.toMap(Classroom::getId, Classroom::getName));
        Map<Long, String> userNames = userIds.isEmpty() ? Collections.emptyMap()
                : sysUserService.listByIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));

        List<Map<String, Object>> records = pageRecords.stream().map(r -> {
            Map<String, Object> map = BeanUtils.toMap(r);
            map.put("classroom_name", roomNames.getOrDefault(r.getClassroomId(), ""));
            map.put("applicant_name", userNames.getOrDefault(r.getApplicantId(), ""));
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", ipage.getTotal());
        result.put("records", records);
        return Result.success(result);
    }

    @Log("申请预约教室")
    @Operation(summary = "申请教室预约")
    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody ClassroomReservation reservation) {
        // 申请人以当前登录用户为准，忽略请求体传入的 applicantId
        reservation.setApplicantId(StpUtil.getLoginIdAsLong());
        ClassroomReservation data = classroomReservationService.createReservation(reservation);
        return Result.success(BeanUtils.toMap(data));
    }

    @SaCheckRole(value = {"admin", "jw"}, mode = SaMode.OR)
    @Log("审批教室预约")
    @Operation(summary = "审批教室预约")
    @PostMapping("/{id}/approve")
    public Result<Map<String, Object>> approve(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String status = (String) body.get("status");
        String remark = (String) body.get("remark");
        // 审批人以当前登录用户为准，不接受请求体传入，防止伪造审批记录
        Long approverId = StpUtil.getLoginIdAsLong();

        ClassroomReservation data = classroomReservationService.approveReservation(id, status, remark, approverId);
        return Result.success(BeanUtils.toMap(data));
    }

    @Log("取消教室预约")
    @Operation(summary = "取消教室预约")
    @PostMapping("/{id}/cancel")
    public Result<Map<String, Object>> cancel(@PathVariable Long id) {
        ClassroomReservation r = classroomReservationService.getById(id);
        if (r == null) {
            throw new CustomException("预约记录不存在");
        }
        // 仅本人或管理员/教务可取消，防止越权取消他人预约
        boolean isOwner = r.getApplicantId() != null
                && r.getApplicantId().equals(StpUtil.getLoginIdAsLong());
        boolean isManager = StpUtil.hasRoleOr("admin", "jw");
        if (!isOwner && !isManager) {
            throw new CustomException(403, "无权取消他人的预约");
        }
        r.setStatus("cancelled");
        classroomReservationService.updateById(r);
        return Result.success(BeanUtils.toMap(r));
    }
}
