package com.icms.service.impl;

import com.icms.common.CustomException;
import com.icms.entity.WorkOrder;
import com.icms.mapper.DeviceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceImplTest {

    @Test
    void repairerCannotHandleAnotherUsersWorkOrder() {
        WorkOrderServiceImpl service = spy(new WorkOrderServiceImpl());
        ReflectionTestUtils.setField(service, "deviceMapper", mock(DeviceMapper.class));

        WorkOrder order = new WorkOrder();
        order.setId(1L);
        order.setStatus("assigned");
        order.setHandlerId(10L);
        doReturn(order).when(service).getById(1L);

        assertThrows(CustomException.class,
                () -> service.handleWorkOrder(1L, "processing", "start", 20L, false));
    }

    @Test
    void rejectsInvalidWorkOrderTransition() {
        WorkOrderServiceImpl service = spy(new WorkOrderServiceImpl());
        ReflectionTestUtils.setField(service, "deviceMapper", mock(DeviceMapper.class));

        WorkOrder order = new WorkOrder();
        order.setId(1L);
        order.setStatus("submitted");
        order.setHandlerId(10L);
        doReturn(order).when(service).getById(1L);

        assertThrows(CustomException.class,
                () -> service.handleWorkOrder(1L, "finished", "done", 10L, false));
    }

    @Test
    void assignedRepairerCanStartWorkOrder() {
        WorkOrderServiceImpl service = spy(new WorkOrderServiceImpl());
        DeviceMapper deviceMapper = mock(DeviceMapper.class);
        ReflectionTestUtils.setField(service, "deviceMapper", deviceMapper);

        WorkOrder order = new WorkOrder();
        order.setId(1L);
        order.setStatus("assigned");
        order.setHandlerId(10L);
        doReturn(order).when(service).getById(1L);
        doReturn(true).when(service).updateById(any(WorkOrder.class));

        assertDoesNotThrow(() -> service.handleWorkOrder(1L, "processing", "start", 10L, false));
    }
}
