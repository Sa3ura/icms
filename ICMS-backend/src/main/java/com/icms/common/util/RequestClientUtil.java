package com.icms.common.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RequestClientUtil {

    private static final Pattern FORWARDED_FOR = Pattern.compile("(?i)(?:^|;)\\s*for=\"?([^;,\\\"]+)\"?");

    private RequestClientUtil() {
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }

        String ip = firstValid(
                request.getHeader("CF-Connecting-IP"),
                request.getHeader("True-Client-IP"),
                request.getHeader("X-Real-IP")
        );
        if (ip != null) {
            return ip;
        }

        ip = firstForwardedFor(request.getHeader("X-Forwarded-For"));
        if (ip != null) {
            return ip;
        }

        ip = parseForwarded(request.getHeader("Forwarded"));
        if (ip != null) {
            return ip;
        }

        return request.getRemoteAddr();
    }

    private static String firstValid(String... values) {
        for (String value : values) {
            String normalized = normalize(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static String firstForwardedFor(String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.split(",");
        for (String part : parts) {
            String normalized = normalize(part);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static String parseForwarded(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = FORWARDED_FOR.matcher(value);
        while (matcher.find()) {
            String normalized = normalize(matcher.group(1));
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String ip = value.trim();
        if (ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            return null;
        }
        if (ip.startsWith("\"") && ip.endsWith("\"") && ip.length() > 1) {
            ip = ip.substring(1, ip.length() - 1);
        }
        if (ip.startsWith("[") && ip.contains("]")) {
            ip = ip.substring(1, ip.indexOf(']'));
            return isPlausibleIp(ip) ? ip : null;
        }
        int colon = ip.indexOf(':');
        if (colon > -1 && ip.indexOf(':', colon + 1) == -1) {
            String port = ip.substring(colon + 1);
            if (port.chars().allMatch(Character::isDigit)) {
                ip = ip.substring(0, colon);
            }
        }
        return isPlausibleIp(ip) ? ip : null;
    }

    private static boolean isPlausibleIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        if (ip.contains(".")) {
            String[] parts = ip.split("\\.", -1);
            if (parts.length != 4) {
                return false;
            }
            for (String part : parts) {
                if (part.isEmpty() || part.length() > 3 || !part.chars().allMatch(Character::isDigit)) {
                    return false;
                }
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) {
                    return false;
                }
            }
            return true;
        }
        return ip.contains(":") && ip.matches("[0-9a-fA-F:.%]+");
    }
}
