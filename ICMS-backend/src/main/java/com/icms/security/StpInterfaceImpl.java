package com.icms.security;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class StpInterfaceImpl implements StpInterface {

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        String role = (String) StpUtil.getExtra("role");
        return role != null ? Collections.singletonList(role) : Collections.emptyList();
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Object perms = StpUtil.getExtra("permissions");
        if (perms instanceof List) {
            return (List<String>) perms;
        }
        return new ArrayList<>();
    }
}
