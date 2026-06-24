package com.icms.common;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class BeanUtils {

    public static Map<String, Object> toMap(Object obj) {
        if (obj == null) return null;
        Map<String, Object> map = new HashMap<>();
        try {
            for (Field field : obj.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                String camelName = field.getName();
                String snakeName = camelToSnake(camelName);
                Object value = field.get(obj);
                map.put(snakeName, value);
            }
        } catch (Exception e) {
            // fallback
        }
        return map;
    }

    private static String camelToSnake(String camel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
