package com.icms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("classroom")
public class Classroom {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long campusId;
    private Long buildingId;
    private Integer floorNo;
    private String roomNo;
    private String name;
    private Integer capacity;
    private String roomType;
    private String status;
    private String openTime;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Schema(hidden = true)
    @TableLogic
    private Integer deleted;
}
