package com.icms.controller;

import com.icms.common.annotation.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icms.common.Result;
import com.icms.entity.Classroom;
import com.icms.entity.Device;
import com.icms.service.ClassroomService;
import com.icms.service.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private ClassroomService classroomService;

    @GetMapping
    public Result<List<Map<String, Object>>> list(
            @RequestParam(required = false) Long classroom_id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String device_type,
            @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<Device> query = new LambdaQueryWrapper<Device>();
        if (classroom_id != null) query.eq(Device::getClassroomId, classroom_id);
        if (status != null && !status.isEmpty()) query.eq(Device::getStatus, status);
        if (device_type != null && !device_type.isEmpty()) query.eq(Device::getDeviceType, device_type);
        if (keyword != null && !keyword.isEmpty()) {
            query.and(q -> q.like(Device::getDeviceName, keyword).or().like(Device::getDeviceCode, keyword));
        }

        List<Device> list = deviceService.list(query);
        List<Classroom> rooms = classroomService.list();
        
        // 关联获取教室名称
        List<Map<String, Object>> result = list.stream().map(d -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", d.getId());
            map.put("classroomId", d.getClassroomId());
            map.put("deviceCode", d.getDeviceCode());
            map.put("deviceName", d.getDeviceName());
            map.put("deviceType", d.getDeviceType());
            map.put("brand", d.getBrand());
            map.put("model", d.getModel());
            map.put("status", d.getStatus());
            map.put("purchaseDate", d.getPurchaseDate());
            map.put("lastCheckTime", d.getLastCheckTime());
            
            Classroom r = rooms.stream().filter(cr -> cr.getId().equals(d.getClassroomId())).findFirst().orElse(null);
            map.put("classroom_name", r != null ? r.getName() : "未指派");
            return map;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    @Log("录入或修改设备档案")
    @PostMapping
    public Result<Device> save(@RequestBody Device device) {
        deviceService.saveOrUpdate(device);
        return Result.success(device);
    }

    @Log("删除设备档案")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        deviceService.removeById(id);
        return Result.success();
    }
}
