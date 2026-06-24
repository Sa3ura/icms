package com.icms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.icms.entity.Classroom;

import java.time.LocalDate;
import java.util.List;

public interface ClassroomService extends IService<Classroom> {
    List<Classroom> getFreeClassrooms(LocalDate date, Integer startSection, Integer endSection, Long campusId, Integer minCapacity, List<String> devices);
    boolean checkConflict(Long classroomId, LocalDate date, Integer startSection, Integer endSection);
}
