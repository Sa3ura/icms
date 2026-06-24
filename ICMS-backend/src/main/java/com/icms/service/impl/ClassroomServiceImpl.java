package com.icms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.icms.entity.Classroom;
import com.icms.entity.ClassroomReservation;
import com.icms.entity.CourseSchedule;
import com.icms.entity.Device;
import com.icms.mapper.ClassroomMapper;
import com.icms.mapper.ClassroomReservationMapper;
import com.icms.mapper.CourseScheduleMapper;
import com.icms.mapper.DeviceMapper;
import com.icms.service.ClassroomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClassroomServiceImpl extends ServiceImpl<ClassroomMapper, Classroom> implements ClassroomService {

    @Autowired
    private CourseScheduleMapper courseScheduleMapper;

    @Autowired
    private ClassroomReservationMapper classroomReservationMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Override
    public List<Classroom> getFreeClassrooms(LocalDate date, Integer startSection, Integer endSection, Long campusId, Integer minCapacity, List<String> devices) {
        // 1. 获取所有可用教室
        LambdaQueryWrapper<Classroom> query = new LambdaQueryWrapper<Classroom>()
                .eq(Classroom::getStatus, "available");

        if (campusId != null) {
            query.eq(Classroom::getCampusId, campusId);
        }
        if (minCapacity != null) {
            query.ge(Classroom::getCapacity, minCapacity);
        }

        List<Classroom> classrooms = this.list(query);

        // 2. 配套设备筛选
        if (devices != null && !devices.isEmpty()) {
            classrooms = classrooms.stream().filter(c -> {
                List<Device> roomDevices = deviceMapper.selectList(new LambdaQueryWrapper<Device>()
                        .eq(Device::getClassroomId, c.getId())
                        .eq(Device::getStatus, "normal"));
                return devices.stream().allMatch(reqType ->
                        roomDevices.stream().anyMatch(d -> d.getDeviceType().equals(reqType))
                );
            }).collect(Collectors.toList());
        }

        // 3. 冲突过滤：排课与预约
        return classrooms.stream().filter(c -> 
                !checkConflict(c.getId(), date, startSection, endSection)
        ).collect(Collectors.toList());
    }

    @Override
    public boolean checkConflict(Long classroomId, LocalDate date, Integer startSection, Integer endSection) {
        // 1. 检查排课占用
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        int weekday = dayOfWeek.getValue(); // 1-7 (周一-周日)

        // 节次冲突条件: new_start <= existing_end AND new_end >= existing_start
        Long scheduleCount = courseScheduleMapper.selectCount(new LambdaQueryWrapper<CourseSchedule>()
                .eq(CourseSchedule::getClassroomId, classroomId)
                .eq(CourseSchedule::getWeekday, weekday)
                .le(CourseSchedule::getStartSection, endSection)
                .ge(CourseSchedule::getEndSection, startSection)
                .eq(CourseSchedule::getStatus, "published"));

        if (scheduleCount > 0) {
            return true;
        }

        // 2. 检查已审批通过的预约占用
        Long reservationCount = classroomReservationMapper.selectCount(new LambdaQueryWrapper<ClassroomReservation>()
                .eq(ClassroomReservation::getClassroomId, classroomId)
                .eq(ClassroomReservation::getReservationDate, date)
                .eq(ClassroomReservation::getStatus, "approved")
                .le(ClassroomReservation::getStartSection, endSection)
                .ge(ClassroomReservation::getEndSection, startSection));

        return reservationCount > 0;
    }
}
