package com.icms.controller;

import com.icms.common.annotation.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icms.common.BeanUtils;
import com.icms.common.Result;
import com.icms.entity.Classroom;
import com.icms.entity.Device;
import com.icms.entity.SysUser;
import com.icms.entity.WorkOrder;
import com.icms.service.ClassroomService;
import com.icms.service.DeviceService;
import com.icms.service.SysUserService;
import com.icms.service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    @Autowired
    private WorkOrderService workOrderService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private ClassroomService classroomService;

    @Autowired
    private SysUserService sysUserService;

    @GetMapping
    public Result<List<Map<String, Object>>> list(
            @RequestParam(required = false) Long reporter_id,
            @RequestParam(required = false) Long handler_id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fault_level,
            @RequestParam(required = false) Long classroom_id) {

        LambdaQueryWrapper<WorkOrder> query = new LambdaQueryWrapper<WorkOrder>();
        if (reporter_id != null) query.eq(WorkOrder::getReporterId, reporter_id);
        if (handler_id != null) query.eq(WorkOrder::getHandlerId, handler_id);
        if (status != null && !status.isEmpty()) query.eq(WorkOrder::getStatus, status);
        if (fault_level != null && !fault_level.isEmpty()) query.eq(WorkOrder::getFaultLevel, fault_level);
        if (classroom_id != null) query.eq(WorkOrder::getClassroomId, classroom_id);

        query.orderByDesc(WorkOrder::getId);
        List<WorkOrder> list = workOrderService.list(query);
        List<Classroom> rooms = classroomService.list();
        List<Device> devices = deviceService.list();
        List<SysUser> users = sysUserService.list();

        List<Map<String, Object>> result = list.stream().map(w -> {
            Map<String, Object> map = BeanUtils.toMap(w);
            Classroom r = rooms.stream().filter(cr -> cr.getId().equals(w.getClassroomId())).findFirst().orElse(null);
            map.put("classroom_name", r != null ? r.getName() : "");
            Device d = devices.stream().filter(dv -> dv.getId().equals(w.getDeviceId())).findFirst().orElse(null);
            map.put("device_name", d != null ? d.getDeviceName() : "");
            SysUser reporter = users.stream().filter(u -> u.getId().equals(w.getReporterId())).findFirst().orElse(null);
            map.put("reporter_name", reporter != null ? reporter.getRealName() : "");
            SysUser handler = users.stream().filter(u -> u.getId().equals(w.getHandlerId())).findFirst().orElse(null);
            map.put("handler_name", handler != null ? handler.getRealName() : "");
            return map;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    @Log("提交设备故障报修")
    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody WorkOrder order) {
        WorkOrder data = workOrderService.createWorkOrder(order);
        return Result.success(BeanUtils.toMap(data));
    }

    @Log("指派故障工单")
    @PostMapping("/{id}/assign")
    public Result<Map<String, Object>> assign(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Number handlerIdNum = (Number) body.get("handlerId");
        Long handlerId = handlerIdNum.longValue();
        WorkOrder data = workOrderService.assignWorkOrder(id, handlerId);
        return Result.success(BeanUtils.toMap(data));
    }

    @Log("处理故障工单")
    @PostMapping("/{id}/handle")
    public Result<Map<String, Object>> handle(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        String result = body.get("handleResult");
        WorkOrder data = workOrderService.handleWorkOrder(id, status, result);
        return Result.success(BeanUtils.toMap(data));
    }

    @Log("关闭故障工单")
    @PostMapping("/{id}/close")
    public Result<Map<String, Object>> close(@PathVariable Long id) {
        WorkOrder order = workOrderService.getById(id);
        if (order != null) {
            order.setStatus("closed");
            workOrderService.updateById(order);
        }
        return Result.success(BeanUtils.toMap(order));
    }
}
