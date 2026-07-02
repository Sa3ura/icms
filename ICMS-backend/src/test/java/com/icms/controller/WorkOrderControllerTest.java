package com.icms.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.icms.common.CustomException;
import com.icms.entity.WorkOrder;
import com.icms.service.ClassroomService;
import com.icms.service.DeviceService;
import com.icms.service.SysUserService;
import com.icms.service.WorkOrderService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkOrderControllerTest {

    @Test
    void reporterCanCloseOwnFinishedWorkOrder() {
        WorkOrderService workOrderService = mock(WorkOrderService.class);
        WorkOrderController controller = newController(workOrderService);

        WorkOrder order = new WorkOrder();
        order.setId(1L);
        order.setReporterId(9L);
        order.setStatus("finished");
        when(workOrderService.getById(1L)).thenReturn(order);
        when(workOrderService.updateById(order)).thenReturn(true);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.hasRoleOr("admin", "jw")).thenReturn(false);
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(9L);

            assertDoesNotThrow(() -> controller.close(1L));
        }

        verify(workOrderService).updateById(order);
    }

    @Test
    void nonReporterCannotCloseWorkOrder() {
        WorkOrderService workOrderService = mock(WorkOrderService.class);
        WorkOrderController controller = newController(workOrderService);

        WorkOrder order = new WorkOrder();
        order.setId(1L);
        order.setReporterId(9L);
        order.setStatus("finished");
        when(workOrderService.getById(1L)).thenReturn(order);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.hasRoleOr("admin", "jw")).thenReturn(false);
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(8L);

            assertThrows(CustomException.class, () -> controller.close(1L));
        }
    }

    @Test
    void cannotCloseUnfinishedWorkOrder() {
        WorkOrderService workOrderService = mock(WorkOrderService.class);
        WorkOrderController controller = newController(workOrderService);

        WorkOrder order = new WorkOrder();
        order.setId(1L);
        order.setReporterId(9L);
        order.setStatus("processing");
        when(workOrderService.getById(1L)).thenReturn(order);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.hasRoleOr("admin", "jw")).thenReturn(false);
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(9L);

            assertThrows(CustomException.class, () -> controller.close(1L));
        }
    }

    private WorkOrderController newController(WorkOrderService workOrderService) {
        WorkOrderController controller = new WorkOrderController();
        ReflectionTestUtils.setField(controller, "workOrderService", workOrderService);
        ReflectionTestUtils.setField(controller, "deviceService", mock(DeviceService.class));
        ReflectionTestUtils.setField(controller, "classroomService", mock(ClassroomService.class));
        ReflectionTestUtils.setField(controller, "sysUserService", mock(SysUserService.class));
        return controller;
    }
}
