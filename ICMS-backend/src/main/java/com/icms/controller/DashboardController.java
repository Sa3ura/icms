package com.icms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icms.common.Result;
import com.icms.entity.Classroom;
import com.icms.entity.ClassroomReservation;
import com.icms.entity.Device;
import com.icms.entity.WorkOrder;
import com.icms.service.ClassroomReservationService;
import com.icms.service.ClassroomService;
import com.icms.service.DeviceService;
import com.icms.service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
}
