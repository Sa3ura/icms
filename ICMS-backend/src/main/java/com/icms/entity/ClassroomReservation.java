package com.icms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("classroom_reservation")
public class ClassroomReservation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applicantId;
    private Long classroomId;
    private LocalDate reservationDate;
    private Integer startSection;
    private Integer endSection;
    private String purpose;
    private Integer attendeeCount;
    private String contactPhone;
    private String status;
    private String approvalRemark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Integer deleted;
}
