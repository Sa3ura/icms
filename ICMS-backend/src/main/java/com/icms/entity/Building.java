package com.icms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("building")
public class Building {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long campusId;
    private String buildingName;
    private Integer floorCount;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Schema(hidden = true)
    @TableLogic
    private Integer deleted;
}
