package com.icms.controller;

import com.icms.common.Result;
import com.icms.entity.Classroom;
import com.icms.service.ClassroomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/environment")
public class EnvironmentController {

    @Autowired
    private ClassroomService classroomService;

    @GetMapping("/records")
    public Result<Map<String, Object>> getRecords(@RequestParam(required = false) Long classroomId) {
        Long cid = classroomId != null ? classroomId : 1L;
        Classroom room = classroomService.getById(cid);
        if (room == null) {
            return Result.error("未找到对应教室");
        }

        Random rand = new Random(cid);
        // 模拟实时指标数据
        Map<String, Object> current = new HashMap<>();
        current.put("classroom_id", cid);
        current.put("classroom_name", room.getName());
        current.put("temperature", getRandVal(rand, 18, 28));
        current.put("humidity", getRandVal(rand, 40, 75));
        current.put("co2", 400 + rand.nextInt(550));
        current.put("pm25", getRandVal(rand, 10, 65));
        current.put("light", 150 + rand.nextInt(450));
        current.put("noise", getRandVal(rand, 30, 70));

        // 模拟 12 小时历史趋势
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalTime now = LocalTime.now();
        for (int i = 11; i >= 0; i--) {
            LocalTime t = now.minusHours(i);
            String timeStr = String.format("%02d:00", t.getHour());

            Map<String, Object> point = new HashMap<>();
            point.put("time", timeStr);
            point.put("temperature", getRandVal(rand, 18, 28));
            point.put("humidity", getRandVal(rand, 40, 75));
            point.put("co2", 400 + rand.nextInt(550));
            point.put("pm25", getRandVal(rand, 10, 65));
            point.put("light", 150 + rand.nextInt(450));
            point.put("noise", getRandVal(rand, 30, 70));
            trend.add(point);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("current", current);
        data.put("trend", trend);
        return Result.success(data);
    }

    private BigDecimal getRandVal(Random rand, double min, double max) {
        double val = min + (max - min) * rand.nextDouble();
        return BigDecimal.valueOf(val).setScale(1, RoundingMode.HALF_UP);
    }
}
