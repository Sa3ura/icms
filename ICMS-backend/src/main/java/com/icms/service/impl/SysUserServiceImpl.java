package com.icms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.icms.common.CustomException;
import com.icms.entity.SysUser;
import com.icms.mapper.SysUserMapper;
import com.icms.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
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
        // 生成随机原文返回给浏览器（写入 httpOnly cookie），库里只存其 SHA-256 哈希。
        // DB 泄露时无法反推原文，避免会话被直接冒用。
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        SysUser user = this.getById(userId);
        if (user != null) {
            user.setRefreshToken(sha256Hex(token));
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
        // 入参是 cookie 里的原文，先哈希再比对库中存储的哈希
        return this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRefreshToken, sha256Hex(refreshToken)));
    }

    // SHA-256 -> 64 位小写 hex（正好匹配 refresh_token VARCHAR(64) 列）
    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
