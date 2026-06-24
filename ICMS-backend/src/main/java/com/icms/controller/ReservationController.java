package com.icms.controller;

import com.icms.common.annotation.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icms.common.Result;
import com.icms.entity.ClassroomReservation;
import com.icms.service.ClassroomReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    @Autowired
    private ClassroomReservationService classroomReservationService;

    @GetMapping
    public Result<List<ClassroomReservation>> list(
            @RequestParam(required = false) Long applicant_id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long classroom_id,
            @RequestParam(required = false) List<String> date_range) {

        LambdaQueryWrapper<ClassroomReservation> query = new LambdaQueryWrapper<ClassroomReservation>();
        if (applicant_id != null) query.eq(ClassroomReservation::getApplicantId, applicant_id);
        if (status != null && !status.isEmpty()) query.eq(ClassroomReservation::getStatus, status);
        if (classroom_id != null) query.eq(ClassroomReservation::getClassroomId, classroom_id);
        if (date_range != null && date_range.size() == 2) {
            query.ge(ClassroomReservation::getReservationDate, LocalDate.parse(date_range.get(0)))
                 .le(ClassroomReservation::getReservationDate, LocalDate.parse(date_range.get(1)));
        }
        
        // 按照 ID 降序排列
        query.orderByDesc(ClassroomReservation::getId);
        return Result.success(classroomReservationService.list(query));
    }

    @Log("申请预约教室")
    @PostMapping
    public Result<ClassroomReservation> create(@RequestBody ClassroomReservation reservation) {
        ClassroomReservation data = classroomReservationService.createReservation(reservation);
        return Result.success(data);
    }

    @Log("审批教室预约")
    @PostMapping("/{id}/approve")
    public Result<ClassroomReservation> approve(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String status = (String) body.get("status");
        String remark = (String) body.get("remark");
        Number approverIdNum = (Number) body.get("approverId");
        Long approverId = approverIdNum != null ? approverIdNum.longValue() : 1L;

        ClassroomReservation data = classroomReservationService.approveReservation(id, status, remark, approverId);
        return Result.success(data);
    }

    @Log("取消教室预约")
    @PostMapping("/{id}/cancel")
    public Result<ClassroomReservation> cancel(@PathVariable Long id) {
        ClassroomReservation r = classroomReservationService.getById(id);
        if (r != null) {
            r.setStatus("cancelled");
            classroomReservationService.updateById(r);
        }
        return Result.success(r);
    }
}
