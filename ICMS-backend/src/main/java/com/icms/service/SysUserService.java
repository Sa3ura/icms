package com.icms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.icms.entity.SysUser;

public interface SysUserService extends IService<SysUser> {
    SysUser validateLogin(String username, String password);

    String rotateRefreshToken(Long userId);

    void clearRefreshToken(Long userId);

    SysUser findByRefreshToken(String refreshToken);
}
