package com.icms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.icms.entity.SysUser;

import java.util.Map;

public interface SysUserService extends IService<SysUser> {
    Map<String, Object> login(String username, String password);
}
