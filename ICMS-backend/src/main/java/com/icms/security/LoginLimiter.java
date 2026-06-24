package com.icms.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LoginLimiter {

    private static final Map<String, int[]> attempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final int WINDOW_MINUTES = 5;
    private static final int BLOCK_MINUTES = 15;

    // 检查是否被限制
    public static boolean isBlocked(String key) {
        int[] record = attempts.get(key);
        if (record == null) return false;
        long now = System.currentTimeMillis();
        if (record[2] > 0 && now < record[2]) {
            return true; // 仍在封锁期
        }
        return false;
    }

    // 获取剩余封锁秒数
    public static long getBlockSeconds(String key) {
        int[] record = attempts.get(key);
        if (record == null) return 0;
        long remaining = record[2] - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }

    // 记录一次失败
    public static void recordFailure(String key) {
        long now = System.currentTimeMillis();
        int[] record = attempts.computeIfAbsent(key, k -> new int[3]);
        long windowStart = now - WINDOW_MINUTES * 60_000L;

        if (record[1] < windowStart) {
            record[0] = 0;
        }
        record[0]++;
        record[1] = (int) now;

        if (record[0] >= MAX_ATTEMPTS) {
            record[2] = (int) (now + BLOCK_MINUTES * 60_000L);
        }
    }

    // 登录成功清除记录
    public static void clear(String key) {
        attempts.remove(key);
    }

    // 获取复合 key (username + IP)
    public static String buildKey(String username, String ip) {
        return ip + "::" + username;
    }
}
