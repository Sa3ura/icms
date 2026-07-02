package com.icms.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.icms.common.Result;
import com.icms.service.ClassroomReservationService;
import com.icms.service.ClassroomService;
import com.icms.service.CourseScheduleService;
import com.icms.service.DeviceService;
import com.icms.service.WorkOrderService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class DashboardControllerTest {

    @Test
    void dashboardStatsAreScopedForRegularUsers() {
        ClassroomService classroomService = mock(ClassroomService.class);
        ClassroomReservationService reservationService = mock(ClassroomReservationService.class);
        WorkOrderService workOrderService = mock(WorkOrderService.class);
        DeviceService deviceService = mock(DeviceService.class);
        DashboardController controller = newController(
                classroomService,
                reservationService,
                workOrderService,
                deviceService);

        when(classroomService.count()).thenReturn(12L);
        when(deviceService.count(any())).thenReturn(3L);
        when(reservationService.count(any())).thenReturn(4L);
        when(workOrderService.count(any())).thenReturn(5L);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.hasRoleOr("admin", "jw")).thenReturn(false);
            stp.when(() -> StpUtil.hasRole("repairer")).thenReturn(false);
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(9L);

            Result<Map<String, Object>> result = controller.getDashboardStats();

            assertEquals(12L, result.getData().get("classroomTotal"));
            assertEquals(4L, result.getData().get("todayReservations"));
            assertEquals(0L, result.getData().get("pendingApprovals"));
            assertEquals(5L, result.getData().get("pendingWorkOrders"));
            assertEquals(3L, result.getData().get("faultyDevices"));
        }
    }

    private DashboardController newController(
            ClassroomService classroomService,
            ClassroomReservationService reservationService,
            WorkOrderService workOrderService,
            DeviceService deviceService) {
        DashboardController controller = new DashboardController();
        ReflectionTestUtils.setField(controller, "classroomService", classroomService);
        ReflectionTestUtils.setField(controller, "classroomReservationService", reservationService);
        ReflectionTestUtils.setField(controller, "workOrderService", workOrderService);
        ReflectionTestUtils.setField(controller, "deviceService", deviceService);
        ReflectionTestUtils.setField(controller, "courseScheduleService", mock(CourseScheduleService.class));
        return controller;
    }
}
