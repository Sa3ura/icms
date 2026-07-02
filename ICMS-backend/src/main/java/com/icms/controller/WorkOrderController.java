package com.icms.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import com.icms.common.annotation.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icms.common.BeanUtils;
import com.icms.common.CustomException;
import com.icms.common.Result;
import com.icms.entity.Classroom;
import com.icms.entity.Device;
import com.icms.entity.SysUser;
import com.icms.entity.WorkOrder;
import com.icms.service.ClassroomService;
import com.icms.service.DeviceService;
import com.icms.service.SysUserService;
import com.icms.service.WorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "报修工单")
@SaCheckLogin
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

    @Operation(summary = "分页查询报修工单")
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) Long reporter_id,
            @RequestParam(required = false) Long handler_id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fault_level,
            @RequestParam(required = false) Long classroom_id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        LambdaQueryWrapper<WorkOrder> query = new LambdaQueryWrapper<WorkOrder>();
        if (StpUtil.hasRoleOr("admin", "jw")) {
            if (reporter_id != null) query.eq(WorkOrder::getReporterId, reporter_id);
            if (handler_id != null) query.eq(WorkOrder::getHandlerId, handler_id);
        } else if (StpUtil.hasRole("repairer")) {
            query.eq(WorkOrder::getHandlerId, StpUtil.getLoginIdAsLong());
        } else {
            query.eq(WorkOrder::getReporterId, StpUtil.getLoginIdAsLong());
        }
        if (status != null && !status.isEmpty()) query.eq(WorkOrder::getStatus, status);
        if (fault_level != null && !fault_level.isEmpty()) query.eq(WorkOrder::getFaultLevel, fault_level);
        if (classroom_id != null) query.eq(WorkOrder::getClassroomId, classroom_id);

        query.orderByDesc(WorkOrder::getId);
        Page<WorkOrder> mpPage = new Page<>(page, pageSize);
        IPage<WorkOrder> ipage = workOrderService.page(mpPage, query);

        // 只按当前页出现过的 id 反查字典，避免一次列表请求把 rooms/devices/users 全表拉进内存。
        // pageSize 通常 10-50，走主键 IN，索引命中，SQL 少而快。
        List<WorkOrder> pageRecords = ipage.getRecords();
        Set<Long> roomIds = pageRecords.stream().map(WorkOrder::getClassroomId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> deviceIds = pageRecords.stream().map(WorkOrder::getDeviceId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> userIds = new HashSet<>();
        for (WorkOrder w : pageRecords) {
            if (w.getReporterId() != null) userIds.add(w.getReporterId());
            if (w.getHandlerId() != null) userIds.add(w.getHandlerId());
        }

        Map<Long, String> roomNames = roomIds.isEmpty() ? Collections.emptyMap()
                : classroomService.listByIds(roomIds).stream()
                        .collect(Collectors.toMap(Classroom::getId, Classroom::getName));
        Map<Long, String> deviceNames = deviceIds.isEmpty() ? Collections.emptyMap()
                : deviceService.listByIds(deviceIds).stream()
                        .collect(Collectors.toMap(Device::getId, Device::getDeviceName));
        Map<Long, String> userNames = userIds.isEmpty() ? Collections.emptyMap()
                : sysUserService.listByIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));

        List<Map<String, Object>> records = pageRecords.stream().map(w -> {
            Map<String, Object> map = BeanUtils.toMap(w);
            map.put("classroom_name", roomNames.getOrDefault(w.getClassroomId(), ""));
            map.put("device_name", deviceNames.getOrDefault(w.getDeviceId(), ""));
            map.put("reporter_name", userNames.getOrDefault(w.getReporterId(), ""));
            map.put("handler_name", userNames.getOrDefault(w.getHandlerId(), ""));
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", ipage.getTotal());
        result.put("records", records);
        return Result.success(result);
    }

    @Log("提交设备故障报修")
    @Operation(summary = "提交设备故障报修")
    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody WorkOrder order) {
        // 报修人以当前登录用户为准，忽略请求体传入的 reporterId，防止伪造他人身份
        order.setReporterId(StpUtil.getLoginIdAsLong());
        WorkOrder data = workOrderService.createWorkOrder(order);
        return Result.success(BeanUtils.toMap(data));
    }

    @SaCheckRole(value = {"admin", "jw"}, mode = SaMode.OR)
    @Log("指派故障工单")
    @Operation(summary = "指派维修人员")
    @PostMapping("/{id}/assign")
    public Result<Map<String, Object>> assign(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Number handlerIdNum = (Number) body.get("handlerId");
        if (handlerIdNum == null) {
            throw new CustomException("请指定处理人");
        }
        Long handlerId = handlerIdNum.longValue();
        WorkOrder data = workOrderService.assignWorkOrder(id, handlerId);
        return Result.success(BeanUtils.toMap(data));
    }

    @SaCheckRole(value = {"admin", "jw", "repairer"}, mode = SaMode.OR)
    @Log("处理故障工单")
    @Operation(summary = "处理报修工单")
    @PostMapping("/{id}/handle")
    public Result<Map<String, Object>> handle(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        String result = body.get("handleResult");
        if (!"processing".equals(status) && !"finished".equals(status)) {
            throw new CustomException("Invalid work order status");
        }
        WorkOrder data = workOrderService.handleWorkOrder(
                id,
                status,
                result,
                StpUtil.getLoginIdAsLong(),
                StpUtil.hasRoleOr("admin", "jw"));
        return Result.success(BeanUtils.toMap(data));
    }

    @SaCheckRole(value = {"admin", "jw"}, mode = SaMode.OR)
    @Log("关闭故障工单")
    @Operation(summary = "关闭报修工单")
    @PostMapping("/{id}/close")
    public Result<Map<String, Object>> close(@PathVariable Long id) {
        WorkOrder order = workOrderService.getById(id);
        if (order == null) {
            throw new CustomException("Work order not found");
        }
        order.setStatus("closed");
        order.setUpdatedAt(java.time.LocalDateTime.now());
        workOrderService.updateById(order);
        return Result.success(BeanUtils.toMap(order));
    }
}
