package com.shortlink.analytics.util;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

// Utility for parsing User-Agent, Referrer, and deriving privacy-conscious visitor hashes.
@Slf4j
public final class AnalyticsParserUtil {

    private AnalyticsParserUtil() {
        // Prevent instantiation
    }

    // Classifies device type into MOBILE, DESKTOP, TABLET, or UNKNOWN.
    public static String classifyDevice(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "UNKNOWN";
        }
        String ua = userAgent.toLowerCase(Locale.ROOT);

        if (ua.contains("ipad") || ua.contains("tablet") || ua.contains("playbook")
                || ua.contains("silk") || ua.contains("kindle")
                || (ua.contains("android") && !ua.contains("mobile"))) {
            return "TABLET";
        }

        if (ua.contains("mobile") || ua.contains("iphone") || ua.contains("ipod")
                || ua.contains("android") || ua.contains("blackberry")
                || ua.contains("windows phone") || ua.contains("opera mini")) {
            return "MOBILE";
        }

        if (ua.contains("windows nt") || ua.contains("macintosh") || ua.contains("mac os x")
                || (ua.contains("linux") && !ua.contains("android")) || ua.contains("cros")) {
            return "DESKTOP";
        }

        return "UNKNOWN";
    }

    // Classifies browser into Chrome, Safari, Edge, Firefox, Opera, Other, or Unknown.
    public static String classifyBrowser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        String ua = userAgent.toLowerCase(Locale.ROOT);

        if (ua.contains("edg/") || ua.contains("edge/")) {
            return "Edge";
        }
        if (ua.contains("opr/") || ua.contains("opera/")) {
            return "Opera";
        }
        if (ua.contains("chrome/") || ua.contains("crios/")) {
            return "Chrome";
        }
        if (ua.contains("safari/") && !ua.contains("chrome/") && !ua.contains("android")) {
            return "Safari";
        }
        if (ua.contains("firefox/") || ua.contains("fxios/")) {
            return "Firefox";
        }

        return "Other";
    }

    // Classifies operating system into Android, iOS, Windows, macOS, Linux, Other, or Unknown.
    public static String classifyOperatingSystem(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        String ua = userAgent.toLowerCase(Locale.ROOT);

        if (ua.contains("android")) {
            return "Android";
        }
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ipod") || ua.contains("cpu os")) {
            return "iOS";
        }
        if (ua.contains("windows nt") || ua.contains("windows")) {
            return "Windows";
        }
        if (ua.contains("macintosh") || ua.contains("mac os x") || ua.contains("macos")) {
            return "macOS";
        }
        if (ua.contains("linux") && !ua.contains("android")) {
            return "Linux";
        }

        return "Other";
    }

    // Converts HTTP Referer header into a clean, human-friendly traffic source name.
    public static String classifyRefererSource(String referer) {
        if (referer == null || referer.isBlank()) {
            return "Direct";
        }

        try {
            URI uri = URI.create(referer.trim());
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return "Direct";
            }
            host = host.toLowerCase(Locale.ROOT);
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }

            if (host.contains("google.")) return "Google";
            if (host.contains("instagram.")) return "Instagram";
            if (host.contains("facebook.") || host.contains("fb.")) return "Facebook";
            if (host.contains("linkedin.") || host.contains("lnkd.in")) return "LinkedIn";
            if (host.contains("twitter.") || host.contains("t.co") || host.contains("x.com")) return "X";
            if (host.contains("youtube.") || host.contains("youtu.be")) return "YouTube";
            if (host.contains("reddit.")) return "Reddit";
            if (host.contains("github.")) return "GitHub";
            if (host.contains("tiktok.")) return "TikTok";
            if (host.contains("whatsapp.")) return "WhatsApp";
            if (host.contains("telegram.") || host.contains("t.me")) return "Telegram";
            if (host.contains("pinterest.")) return "Pinterest";

            return host;
        } catch (Exception e) {
            return "Other";
        }
    }

    // Computes a SHA-256 hash of IP + User-Agent for privacy-preserving unique visitor counting.
    public static String generateVisitorHash(String ipAddress, String userAgent) {
        String raw = (ipAddress != null ? ipAddress.trim() : "unknown") + "|"
                + (userAgent != null ? userAgent.trim() : "unknown");

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found", e);
            return Integer.toHexString(raw.hashCode());
        }
    }
}
