package com.icms.security;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CaptchaUtil {

    private static final SecureRandom random = new SecureRandom();
    private static final Map<String, CaptchaEntry> store = new ConcurrentHashMap<>();
    private static final long TTL_MS = 120_000; // 2 分钟有效
    private static final int WIDTH = 120;
    private static final int HEIGHT = 48;
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Color[] COLORS = {
        new Color(0x4f, 0x46, 0xe5),
        new Color(0x10, 0xb9, 0x81),
        new Color(0xf5, 0x9e, 0x0b),
        new Color(0xef, 0x44, 0x44),
        new Color(0x8b, 0x5c, 0xf6)
    };

    private static class CaptchaEntry {
        String code;
        long createdAt;
        CaptchaEntry(String code) { this.code = code; this.createdAt = System.currentTimeMillis(); }
    }

    // 清理过期验证码 (每次生成时调用)
    private static void cleanExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> now - e.getValue().createdAt > TTL_MS);
    }

    // 生成验证码字符串
    private static String randomCode() {
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    // 生成 SVG 字符串
    public static String generateSVG(String captchaId) {
        String code = randomCode();
        store.put(captchaId, new CaptchaEntry(code));
        cleanExpired();
        return renderSVG(code);
    }

    // 校验验证码
    public static boolean validate(String captchaId, String userCode) {
        CaptchaEntry entry = store.remove(captchaId);
        if (entry == null) return false;
        if (System.currentTimeMillis() - entry.createdAt > TTL_MS) return false;
        return entry.code.equalsIgnoreCase(userCode);
    }

    // 生成新的验证码 ID
    public static String newCaptchaId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // --- SVG 渲染 ---
    private static String renderSVG(String code) {
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' width='").append(WIDTH).append("' height='").append(HEIGHT).append("'>");

        // 背景
        svg.append("<rect width='100%' height='100%' fill='#f8f9fa' rx='6'/>");

        // 干扰线
        for (int i = 0; i < 3; i++) {
            int x1 = random.nextInt(WIDTH);
            int y1 = random.nextInt(HEIGHT);
            int x2 = random.nextInt(WIDTH);
            int y2 = random.nextInt(HEIGHT);
            Color c = COLORS[random.nextInt(COLORS.length)];
            svg.append("<line x1='").append(x1).append("' y1='").append(y1)
                .append("' x2='").append(x2).append("' y2='").append(y2)
                .append("' stroke='").append(toHex(c)).append("' stroke-width='1' opacity='0.3'/>");
        }

        // 干扰点
        for (int i = 0; i < 20; i++) {
            int cx = random.nextInt(WIDTH);
            int cy = random.nextInt(HEIGHT);
            svg.append("<circle cx='").append(cx).append("' cy='").append(cy)
                .append("' r='1' fill='").append(toHex(COLORS[random.nextInt(COLORS.length)])).append("' opacity='0.4'/>");
        }

        // 字符 + 旋转
        int charWidth = WIDTH / 4;
        for (int i = 0; i < 4; i++) {
            String ch = String.valueOf(code.charAt(i));
            Color c = COLORS[i % COLORS.length];
            int x = i * charWidth + charWidth / 2;
            int y = HEIGHT / 2;
            int rotation = random.nextInt(30) - 15;

            svg.append("<text x='").append(x).append("' y='").append(y)
                .append("' font-size='28' font-family='Arial, sans-serif' font-weight='bold'")
                .append(" fill='").append(toHex(c)).append("' text-anchor='middle' dominant-baseline='central'")
                .append(" transform='rotate(").append(rotation).append(" ").append(x).append(" ").append(y).append(")'")
                .append(">").append(ch).append("</text>");
        }

        svg.append("</svg>");
        return svg.toString();
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}
