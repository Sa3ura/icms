package com.icms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.icms.entity.WorkOrder;

public interface WorkOrderService extends IService<WorkOrder> {
    WorkOrder createWorkOrder(WorkOrder workOrder);
    WorkOrder assignWorkOrder(Long id, Long handlerId);
    WorkOrder handleWorkOrder(Long id, String status, String result);
}
