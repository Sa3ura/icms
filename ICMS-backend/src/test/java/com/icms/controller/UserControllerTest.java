package com.icms.controller;

import com.icms.common.CustomException;
import com.icms.entity.SysUser;
import com.icms.service.OperationLogService;
import com.icms.service.SysUserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerTest {

    @Test
    void createsUserWithEncodedPassword() {
        SysUserService sysUserService = mock(SysUserService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UserController controller = newController(sysUserService, passwordEncoder);

        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        when(sysUserService.saveOrUpdate(any(SysUser.class))).thenReturn(true);

        Map<String, Object> body = new HashMap<>();
        body.put("username", "teacher");
        body.put("real_name", "Teacher");
        body.put("user_type", "teacher");
        body.put("password", "secret123");

        controller.save(body);

        org.mockito.ArgumentCaptor<SysUser> captor = org.mockito.ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserService).saveOrUpdate(captor.capture());
        SysUser saved = captor.getValue();
        assertEquals("teacher", saved.getUsername());
        assertEquals("Teacher", saved.getRealName());
        assertEquals("teacher", saved.getUserType());
        assertEquals("encoded-secret", saved.getPassword());
        assertEquals(1, saved.getStatus());
    }

    @Test
    void updateIgnoresSensitiveFields() {
        SysUserService sysUserService = mock(SysUserService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UserController controller = newController(sysUserService, passwordEncoder);

        SysUser existing = new SysUser();
        existing.setId(7L);
        existing.setUsername("old");
        existing.setPassword("existing-hash");
        existing.setRefreshToken("existing-refresh");
        existing.setDeleted(0);
        when(sysUserService.getById(7L)).thenReturn(existing);
        when(sysUserService.saveOrUpdate(any(SysUser.class))).thenReturn(true);

        Map<String, Object> body = new HashMap<>();
        body.put("id", 7L);
        body.put("username", "new");
        body.put("password", "plain-new");
        body.put("refresh_token", "attacker-refresh");
        body.put("deleted", 1);

        controller.save(body);

        org.mockito.ArgumentCaptor<SysUser> captor = org.mockito.ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserService).saveOrUpdate(captor.capture());
        SysUser saved = captor.getValue();
        assertEquals("new", saved.getUsername());
        assertEquals("existing-hash", saved.getPassword());
        assertEquals("existing-refresh", saved.getRefreshToken());
        assertEquals(0, saved.getDeleted());
    }

    @Test
    void deletesExistingNonAdminUser() {
        SysUserService sysUserService = mock(SysUserService.class);
        UserController controller = newController(sysUserService, mock(PasswordEncoder.class));

        SysUser existing = new SysUser();
        existing.setId(7L);
        existing.setUsername("teacher");
        when(sysUserService.getById(7L)).thenReturn(existing);
        when(sysUserService.removeById(7L)).thenReturn(true);

        controller.delete(7L);

        verify(sysUserService).removeById(7L);
    }

    @Test
    void rejectsDeletingAdminUser() {
        SysUserService sysUserService = mock(SysUserService.class);
        UserController controller = newController(sysUserService, mock(PasswordEncoder.class));

        SysUser existing = new SysUser();
        existing.setId(1L);
        existing.setUsername("admin");
        when(sysUserService.getById(1L)).thenReturn(existing);

        assertThrows(CustomException.class, () -> controller.delete(1L));
    }

    private UserController newController(SysUserService sysUserService, PasswordEncoder passwordEncoder) {
        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "sysUserService", sysUserService);
        ReflectionTestUtils.setField(controller, "operationLogService", mock(OperationLogService.class));
        ReflectionTestUtils.setField(controller, "passwordEncoder", passwordEncoder);
        return controller;
    }
}
