package com.icms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("work_order")
public class WorkOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long reporterId;
    private Long classroomId;
    private Long deviceId;
    private String faultTitle;
    private String faultDescription;
    private String faultLevel;
    private String status;
    private Long handlerId;
    private String handleResult;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Schema(hidden = true)
    @TableLogic
    private Integer deleted;
}
