package com.icms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_schedule")
public class CourseSchedule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String semester;
    private Integer weekNo;
    private Integer weekday;
    private Integer startSection;
    private Integer endSection;
    private Long classroomId;
    private String courseCode;
    private String courseName;
    private Long teacherId;
    private String className;
    private Integer studentCount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Schema(hidden = true)
    @TableLogic
    private Integer deleted;
}
