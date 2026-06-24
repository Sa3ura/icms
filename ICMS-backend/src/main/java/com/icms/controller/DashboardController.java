package com.icms.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
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

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboardStats() {
        long classroomTotal = classroomService.count();
        
        long todayReservations = classroomReservationService.count(new LambdaQueryWrapper<ClassroomReservation>()
                .eq(ClassroomReservation::getReservationDate, LocalDate.now()));
                
        long pendingApprovals = classroomReservationService.count(new LambdaQueryWrapper<ClassroomReservation>()
                .eq(ClassroomReservation::getStatus, "pending"));
                
        long pendingWorkOrders = workOrderService.count(new LambdaQueryWrapper<WorkOrder>()
                .in(WorkOrder::getStatus, Arrays.asList("submitted", "assigned", "processing")));
                
        long faultyDevices = deviceService.count(new LambdaQueryWrapper<Device>()
                .eq(Device::getStatus, "fault"));

        Map<String, Object> stats = new HashMap<>();
        stats.put("classroomTotal", classroomTotal);
        stats.put("todayReservations", todayReservations);
        stats.put("pendingApprovals", pendingApprovals);
        stats.put("pendingWorkOrders", pendingWorkOrders);
        stats.put("faultyDevices", faultyDevices);

        return Result.success(stats);
    }

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
