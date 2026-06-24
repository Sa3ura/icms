package com.icms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("campus")
public class Campus {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String campusName;
    private String address;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Integer deleted;
}
