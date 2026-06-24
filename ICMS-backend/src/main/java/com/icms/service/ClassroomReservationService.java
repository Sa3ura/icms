package com.icms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.icms.entity.ClassroomReservation;

public interface ClassroomReservationService extends IService<ClassroomReservation> {
    ClassroomReservation createReservation(ClassroomReservation reservation);
    ClassroomReservation approveReservation(Long id, String status, String remark, Long approverId);
}
