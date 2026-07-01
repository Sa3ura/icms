package com.icms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;

    @JsonIgnore
    @Schema(hidden = true)
    private String password;

    @JsonIgnore
    @Schema(hidden = true)
    private String refreshToken;

    private String realName;
    private String email;
    private String phone;
    private String userType;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Schema(hidden = true)
    @TableLogic
    private Integer deleted;
}
