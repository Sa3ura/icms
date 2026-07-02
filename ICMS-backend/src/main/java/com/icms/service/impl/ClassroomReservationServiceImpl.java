package com.icms.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.icms.common.CustomException;
import com.icms.entity.ApprovalRecord;
import com.icms.entity.Classroom;
import com.icms.entity.ClassroomReservation;
import com.icms.mapper.ApprovalRecordMapper;
import com.icms.mapper.ClassroomMapper;
import com.icms.mapper.ClassroomReservationMapper;
import com.icms.service.ClassroomReservationService;
import com.icms.service.ClassroomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ClassroomReservationServiceImpl extends ServiceImpl<ClassroomReservationMapper, ClassroomReservation> implements ClassroomReservationService {

    @Autowired
    private ClassroomService classroomService;

    @Autowired
    private ClassroomMapper classroomMapper;

    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    @Override
    @Transactional
    public ClassroomReservation createReservation(ClassroomReservation reservation) {
        // 1. 验证教室容量
        Classroom classroom = classroomMapper.selectById(reservation.getClassroomId());
        if (classroom == null) {
            throw new CustomException("教室不存在");
        }
        if (reservation.getAttendeeCount() != null && reservation.getAttendeeCount() > classroom.getCapacity()) {
            throw new CustomException("预约失败：使用人数超过教室额定容量 (" + classroom.getCapacity() + "人)");
        }

        // 2. 验证冲突
        boolean hasConflict = classroomService.checkConflict(
                reservation.getClassroomId(),
                reservation.getReservationDate(),
                reservation.getStartSection(),
                reservation.getEndSection()
        );
        if (hasConflict) {
            throw new CustomException("预约失败：选定时间段内该教室已被占用或排课");
        }

        // 3. 保存预约记录
        reservation.setStatus("pending");
        reservation.setCreatedAt(LocalDateTime.now());
        this.save(reservation);
        return reservation;
    }

    @Override
    @Transactional
    public synchronized ClassroomReservation approveReservation(Long id, String status, String remark, Long approverId) {
        if (!"approved".equals(status) && !"rejected".equals(status)) {
            throw new CustomException("Invalid approval status");
        }
        ClassroomReservation reservation = this.getById(id);
        if (reservation == null) {
            throw new CustomException("预约记录不存在");
        }
        if (!"pending".equals(reservation.getStatus())) {
            throw new CustomException("无法处理该预约：该记录已审批或已取消");
        }

        // 1. 如果同意，重新校验冲突（防并发）
        if ("approved".equals(status)) {
            boolean hasConflict = classroomService.checkConflict(
                    reservation.getClassroomId(),
                    reservation.getReservationDate(),
                    reservation.getStartSection(),
                    reservation.getEndSection()
            );
            if (hasConflict) {
                throw new CustomException("审批失败：该时间段已被其他预约通过或已被排课");
            }
        }

        // 2. 更新状态
        reservation.setStatus(status);
        reservation.setApprovalRemark(remark);
        reservation.setUpdatedAt(LocalDateTime.now());
        boolean updated = this.update(reservation, new LambdaUpdateWrapper<ClassroomReservation>()
                .eq(ClassroomReservation::getId, id)
                .eq(ClassroomReservation::getStatus, "pending"));
        if (!updated) {
            throw new CustomException("预约状态已变化，请刷新后重试");
        }

        // 3. 写入审批记录
        ApprovalRecord record = new ApprovalRecord();
        record.setBusinessType("reservation");
        record.setBusinessId(id);
        record.setApproverId(approverId);
        record.setAction("approved".equals(status) ? "approve" : "reject");
        record.setComment(remark);
        record.setCreatedAt(LocalDateTime.now());
        approvalRecordMapper.insert(record);

        return reservation;
    }
}
