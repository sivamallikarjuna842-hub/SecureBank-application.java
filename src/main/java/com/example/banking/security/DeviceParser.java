package com.example.banking.security;

import java.security.MessageDigest;
import java.util.Locale;

/** Tiny UA parser. Detects OS + browser for session device info. */
public final class DeviceParser {
    private DeviceParser() {}

    public static String osOf(String ua) {
        if (ua == null) return "Unknown";
        ua = ua.toLowerCase();
        if (ua.contains("windows nt 10")) return "Windows 10/11";
        if (ua.contains("windows"))        return "Windows";
        if (ua.contains("mac os x"))       return "macOS";
        if (ua.contains("iphone"))         return "iOS";
        if (ua.contains("android"))        return "Android";
        if (ua.contains("linux"))          return "Linux";
        return "Unknown";
    }

    public static String browserOf(String ua) {
        if (ua == null) return "Unknown";
        ua = ua.toLowerCase();
        if (ua.contains("edg/"))       return "Edge";
        if (ua.contains("chrome/") && !ua.contains("chromium")) return "Chrome";
        if (ua.contains("firefox/"))   return "Firefox";
        if (ua.contains("safari/") && !ua.contains("chrome/")) return "Safari";
        if (ua.contains("opera") || ua.contains("opr/")) return "Opera";
        return "Unknown";
    }

    /** SHA-256 fingerprint of User-Agent for stable device identity. */
    public static String fingerprint(String ua) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((ua == null ? "unknown" : ua).getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format(Locale.ROOT, "%02x", b));
            return sb.substring(0, 32);
        } catch (Exception e) {
            return "fp-error";
        }
    }
}
