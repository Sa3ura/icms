package com.icms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.icms.common.CustomException;
import com.icms.entity.SysUser;
import com.icms.mapper.SysUserMapper;
import com.icms.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private static final SecureRandom secureRandom = new SecureRandom();

    @Override
    public SysUser validateLogin(String username, String password) {
        SysUser user = this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));

        if (user == null) {
            throw new CustomException("用户名或密码错误");
        }

        if (user.getStatus() != 1) {
            throw new CustomException("该账号已被禁用，请联系管理员");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new CustomException("用户名或密码错误");
        }

        return user;
    }

    @Override
    public String rotateRefreshToken(Long userId) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        SysUser user = this.getById(userId);
        if (user != null) {
            user.setRefreshToken(token);
            this.updateById(user);
        }
        return token;
    }

    @Override
    public void clearRefreshToken(Long userId) {
        SysUser user = this.getById(userId);
        if (user != null) {
            user.setRefreshToken(null);
            this.updateById(user);
        }
    }

    @Override
    public SysUser findByRefreshToken(String refreshToken) {
        return this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRefreshToken, refreshToken));
    }
}
