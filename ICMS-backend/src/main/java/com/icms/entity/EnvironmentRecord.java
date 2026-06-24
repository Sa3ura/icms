package com.icms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("environment_record")
public class EnvironmentRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long classroomId;
    private BigDecimal temperature;
    private BigDecimal humidity;
    private Integer co2;
    private BigDecimal pm25;
    private BigDecimal light;
    private BigDecimal noise;
    private LocalDateTime recordTime;
}
