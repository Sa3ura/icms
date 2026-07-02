package com.icms.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icms.common.Result;
import com.icms.entity.Classroom;
import com.icms.entity.ClassroomReservation;
import com.icms.entity.CourseSchedule;
import com.icms.entity.Device;
import com.icms.entity.WorkOrder;
import com.icms.service.ClassroomReservationService;
import com.icms.service.ClassroomService;
import com.icms.service.CourseScheduleService;
import com.icms.service.DeviceService;
import com.icms.service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "统计分析")
@SaCheckLogin
@RestController
@RequestMapping("/api/stats")
public class DashboardController {

    @Autowired
    private ClassroomService classroomService;

    @Autowired
    private ClassroomReservationService classroomReservationService;

    @Autowired
    private WorkOrderService workOrderService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private CourseScheduleService courseScheduleService;

    @Operation(summary = "获取仪表盘统计指标")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboardStats() {
        long classroomTotal = classroomService.count();
        long faultyDevices = deviceService.count(new LambdaQueryWrapper<Device>()
                .eq(Device::getStatus, "fault"));

        long todayReservations;
        long pendingApprovals;
        long pendingWorkOrders;
        if (StpUtil.hasRoleOr("admin", "jw")) {
            todayReservations = classroomReservationService.count(new LambdaQueryWrapper<ClassroomReservation>()
                    .eq(ClassroomReservation::getReservationDate, LocalDate.now()));
            pendingApprovals = classroomReservationService.count(new LambdaQueryWrapper<ClassroomReservation>()
                    .eq(ClassroomReservation::getStatus, "pending"));
            pendingWorkOrders = workOrderService.count(new LambdaQueryWrapper<WorkOrder>()
                    .in(WorkOrder::getStatus, Arrays.asList("submitted", "assigned", "processing")));
        } else if (StpUtil.hasRole("repairer")) {
            Long userId = StpUtil.getLoginIdAsLong();
            todayReservations = 0;
            pendingApprovals = 0;
            pendingWorkOrders = workOrderService.count(new LambdaQueryWrapper<WorkOrder>()
                    .eq(WorkOrder::getHandlerId, userId)
                    .in(WorkOrder::getStatus, Arrays.asList("assigned", "processing")));
        } else {
            Long userId = StpUtil.getLoginIdAsLong();
            todayReservations = classroomReservationService.count(new LambdaQueryWrapper<ClassroomReservation>()
                    .eq(ClassroomReservation::getApplicantId, userId)
                    .eq(ClassroomReservation::getReservationDate, LocalDate.now()));
            pendingApprovals = 0;
            pendingWorkOrders = workOrderService.count(new LambdaQueryWrapper<WorkOrder>()
                    .eq(WorkOrder::getReporterId, userId)
                    .in(WorkOrder::getStatus, Arrays.asList("submitted", "assigned", "processing")));
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("classroomTotal", classroomTotal);
        stats.put("todayReservations", todayReservations);
        stats.put("pendingApprovals", pendingApprovals);
        stats.put("pendingWorkOrders", pendingWorkOrders);
        stats.put("faultyDevices", faultyDevices);

        return Result.success(stats);
    }

    @Operation(summary = "获取近7天空间使用率")
    @GetMapping("/weekly-usage")
    public Result<List<Map<String, Object>>> getWeeklyUsage() {
        long roomTotal = classroomService.count();
        if (roomTotal == 0) roomTotal = 1;

        List<Map<String, Object>> result = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            int weekday = date.getDayOfWeek().getValue();

            Set<Long> occupiedRooms = new HashSet<>();

            List<ClassroomReservation> reservations = classroomReservationService.list(
                    new LambdaQueryWrapper<ClassroomReservation>()
                            .eq(ClassroomReservation::getReservationDate, date)
                            .eq(ClassroomReservation::getStatus, "approved"));
            for (ClassroomReservation r : reservations) {
                occupiedRooms.add(r.getClassroomId());
            }

            List<CourseSchedule> schedules = courseScheduleService.list(
                    new LambdaQueryWrapper<CourseSchedule>()
                            .eq(CourseSchedule::getWeekday, weekday));
            for (CourseSchedule s : schedules) {
                occupiedRooms.add(s.getClassroomId());
            }

            int rate = (int) Math.round(occupiedRooms.size() * 100.0 / roomTotal);

            Map<String, Object> point = new HashMap<>();
            point.put("date", date.toString());
            point.put("dayOfWeek", weekday);
            point.put("rate", Math.min(100, rate));
            result.add(point);
        }

        return Result.success(result);
    }
}
