package com.icms.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NoticeControllerTest {

    @Test
    void readUsersRequiresManagerRole() throws NoSuchMethodException {
        Method method = NoticeController.class.getDeclaredMethod("getReadUsers", Long.class);

        SaCheckRole annotation = method.getAnnotation(SaCheckRole.class);

        assertNotNull(annotation);
        assertArrayEquals(new String[]{"admin", "jw"}, annotation.value());
        assertEquals(SaMode.OR, annotation.mode());
    }
}
