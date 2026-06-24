package com.icms.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.icms.common.annotation.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icms.common.Result;
import com.icms.entity.Classroom;
import com.icms.entity.Device;
import com.icms.service.ClassroomService;
import com.icms.service.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.icms.common.BeanUtils;

import java.util.*;
import java.util.stream.Collectors;

@SaCheckLogin
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private ClassroomService classroomService;

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) Long classroom_id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String device_type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        LambdaQueryWrapper<Device> query = new LambdaQueryWrapper<Device>();
        if (classroom_id != null) query.eq(Device::getClassroomId, classroom_id);
        if (status != null && !status.isEmpty()) query.eq(Device::getStatus, status);
        if (device_type != null && !device_type.isEmpty()) query.eq(Device::getDeviceType, device_type);
        if (keyword != null && !keyword.isEmpty()) {
            query.and(q -> q.like(Device::getDeviceName, keyword).or().like(Device::getDeviceCode, keyword));
        }

        Page<Device> mpPage = new Page<>(page, pageSize);
        IPage<Device> ipage = deviceService.page(mpPage, query);
        List<Classroom> rooms = classroomService.list();

        List<Map<String, Object>> records = ipage.getRecords().stream().map(d -> {
            Map<String, Object> map = BeanUtils.toMap(d);
            Classroom r = rooms.stream().filter(cr -> cr.getId().equals(d.getClassroomId())).findFirst().orElse(null);
            map.put("classroom_name", r != null ? r.getName() : "未指派");
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", ipage.getTotal());
        result.put("records", records);
        return Result.success(result);
    }

    @SaCheckRole({"admin", "jw"})
    @Log("录入或修改设备档案")
    @PostMapping
    public Result<Map<String, Object>> save(@RequestBody Device device) {
        deviceService.saveOrUpdate(device);
        return Result.success(BeanUtils.toMap(device));
    }

    @SaCheckRole({"admin", "jw"})
    @Log("删除设备档案")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        deviceService.removeById(id);
        return Result.success();
    }
}
