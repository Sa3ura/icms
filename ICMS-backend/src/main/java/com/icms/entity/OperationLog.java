package com.icms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String operation;
    private String requestMethod;
    private String requestUrl;
    private String requestIp;
    private String requestParams;
    private String resultStatus;
    private String errorMessage;
    private LocalDateTime createdAt;
}
