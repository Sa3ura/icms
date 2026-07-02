package com.icms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.icms.common.CustomException;
import com.icms.entity.Device;
import com.icms.entity.WorkOrder;
import com.icms.mapper.DeviceMapper;
import com.icms.mapper.WorkOrderMapper;
import com.icms.service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class WorkOrderServiceImpl extends ServiceImpl<WorkOrderMapper, WorkOrder> implements WorkOrderService {

    @Autowired
    private DeviceMapper deviceMapper;

    @Override
    @Transactional
    public WorkOrder createWorkOrder(WorkOrder workOrder) {
        // 1. 自动生成工单号
        String orderNo = "WO-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        workOrder.setOrderNo(orderNo);
        workOrder.setStatus("submitted");
        workOrder.setCreatedAt(LocalDateTime.now());

        // 2. 如果关联了设备，更新设备状态为故障
        if (workOrder.getDeviceId() != null) {
            Device device = deviceMapper.selectById(workOrder.getDeviceId());
            if (device != null) {
                device.setStatus("fault");
                deviceMapper.updateById(device);
            }
        }

        this.save(workOrder);
        return workOrder;
    }

    @Override
    @Transactional
    public WorkOrder assignWorkOrder(Long id, Long handlerId) {
        WorkOrder order = this.getById(id);
        if (order == null) {
            throw new CustomException("工单不存在");
        }
        if (!"submitted".equals(order.getStatus())) {
            throw new CustomException("无法指派：该工单已处于处理中或修复完毕状态");
        }

        order.setStatus("assigned");
        order.setHandlerId(handlerId);
        order.setUpdatedAt(LocalDateTime.now());
        this.updateById(order);

        // 如果关联了设备，更新设备状态为“维修中”
        if (order.getDeviceId() != null) {
            Device device = deviceMapper.selectById(order.getDeviceId());
            if (device != null) {
                device.setStatus("repairing");
                deviceMapper.updateById(device);
            }
        }

        return order;
    }

    @Override
    @Transactional
    public WorkOrder handleWorkOrder(Long id, String status, String result, Long currentUserId, boolean isManager) {
        WorkOrder order = this.getById(id);
        if (order == null) {
            throw new CustomException("工单不存在");
        }
        if (!"processing".equals(status) && !"finished".equals(status)) {
            throw new CustomException("Invalid work order status");
        }
        if (!isManager && (order.getHandlerId() == null || !order.getHandlerId().equals(currentUserId))) {
            throw new CustomException(403, "No permission to handle this work order");
        }
        if ("processing".equals(status) && !"assigned".equals(order.getStatus())) {
            throw new CustomException("Invalid work order transition");
        }
        if ("finished".equals(status)
                && !"assigned".equals(order.getStatus())
                && !"processing".equals(order.getStatus())) {
            throw new CustomException("Invalid work order transition");
        }

        // status: processing(处理中), finished(已修复)
        order.setStatus(status);
        order.setHandleResult(result);
        order.setUpdatedAt(LocalDateTime.now());

        if ("finished".equals(status)) {
            order.setFinishedAt(LocalDateTime.now());
        }

        this.updateById(order);

        // 更新关联设备状态
        if (order.getDeviceId() != null) {
            Device device = deviceMapper.selectById(order.getDeviceId());
            if (device != null) {
                if ("finished".equals(status)) {
                    device.setStatus("normal");
                    device.setLastCheckTime(LocalDateTime.now());
                } else {
                    device.setStatus("repairing");
                }
                deviceMapper.updateById(device);
            }
        }

        return order;
    }
}
