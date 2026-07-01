package com.icms.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icms.common.BeanUtils;
import com.icms.common.Result;
import com.icms.entity.Building;
import com.icms.entity.Campus;
import com.icms.service.BuildingService;
import com.icms.service.CampusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "校区与楼栋")
@SaCheckLogin
@RestController
public class CampusBuildingController {

    @Autowired
    private CampusService campusService;

    @Autowired
    private BuildingService buildingService;

    @Operation(summary = "获取全部校区列表")
    @GetMapping("/api/campuses")
    public Result<List<Map<String, Object>>> getCampuses() {
        List<Campus> list = campusService.list();
        return Result.success(list.stream().map(BeanUtils::toMap).collect(Collectors.toList()));
    }

    @Operation(summary = "获取楼栋列表（可按校区过滤）")
    @GetMapping("/api/buildings")
    public Result<List<Map<String, Object>>> getBuildings(
            @RequestParam(required = false) Long campus_id) {
        LambdaQueryWrapper<Building> query = new LambdaQueryWrapper<>();
        if (campus_id != null) {
            query.eq(Building::getCampusId, campus_id);
        }
        List<Building> list = buildingService.list(query);
        return Result.success(list.stream().map(BeanUtils::toMap).collect(Collectors.toList()));
    }
}
