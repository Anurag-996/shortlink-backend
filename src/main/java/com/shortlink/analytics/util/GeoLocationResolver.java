package com.shortlink.analytics.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;

// Utility class resolving country, region, and city dynamically from proxy headers (Cloudflare, Vercel, Render), browser Accept-Language, and timezones.
public final class GeoLocationResolver {

    public record GeoLocation(String country, String region, String city) {}

    private GeoLocationResolver() {}

    public static GeoLocation resolve(HttpServletRequest request, String ipAddress) {
        String rawCountry = null;
        String rawRegion = null;
        String rawCity = null;
        String rawTimezone = null;
        String rawAcceptLanguage = null;

        if (request != null) {
            // 1. Resolve Country Header (Cloudflare, Vercel, Render, Nginx)
            rawCountry = getFirstHeader(request,
                    "x-render-origin-country",
                    "x-vercel-ip-country",
                    "cf-ipcountry",
                    "x-country-code",
                    "x-country"
            );

            // 2. Resolve Region Header
            rawRegion = getFirstHeader(request,
                    "x-vercel-ip-country-region",
                    "cf-region",
                    "x-region"
            );

            // 3. Resolve City Header
            rawCity = getFirstHeader(request,
                    "x-vercel-ip-city",
                    "cf-ipcity",
                    "x-city"
            );

            // 4. Resolve Timezone Header
            rawTimezone = getFirstHeader(request,
                    "x-vercel-ip-timezone",
                    "cf-timezone",
                    "x-timezone"
            );

            // 5. Resolve Accept-Language Header from client browser
            rawAcceptLanguage = request.getHeader("accept-language");
        }

        boolean isLocal = isLocalOrPrivateIp(ipAddress);

        // --- Process Country ---
        String country = null;

        // Tier 1: CDN / Proxy Country Header (e.g. "IN", "US", "GB")
        if (rawCountry != null && !rawCountry.isBlank() && !isIgnoredCountry(rawCountry)) {
            String code = rawCountry.trim().toUpperCase();
            if (code.length() == 2) {
                String display = Locale.of("", code).getDisplayCountry(Locale.ENGLISH);
                country = (display != null && !display.isBlank()) ? display : code;
            } else {
                country = rawCountry.trim();
            }
        }

        // Tier 2: Browser Accept-Language header (e.g. "en-IN,en;q=0.9" -> "IN" -> "India")
        if (country == null && rawAcceptLanguage != null && !rawAcceptLanguage.isBlank()) {
            country = resolveCountryFromAcceptLanguage(rawAcceptLanguage);
        }

        // Tier 3: Timezone Header (e.g. from Vercel / Cloudflare)
        if (country == null && rawTimezone != null && !rawTimezone.isBlank()) {
            country = resolveCountryFromTimezone(rawTimezone.trim());
        }

        // Tier 4: Local development system timezone (e.g. Asia/Kolkata -> India)
        if (country == null && isLocal) {
            String localTz = ZoneId.systemDefault().getId();
            country = resolveCountryFromTimezone(localTz);
        }

        // Tier 5: System User Country or Default Locale fallback
        if (country == null) {
            if (isLocal) {
                String sysPropCountry = System.getProperty("user.country");
                if (sysPropCountry != null && sysPropCountry.length() == 2) {
                    country = Locale.of("", sysPropCountry.toUpperCase()).getDisplayCountry(Locale.ENGLISH);
                } else {
                    country = "India"; // Default local development country
                }
            } else {
                country = "Global / Direct";
            }
        }

        // --- Process City ---
        String city = null;
        if (rawCity != null && !rawCity.isBlank() && !"UNKNOWN".equalsIgnoreCase(rawCity)) {
            try {
                city = URLDecoder.decode(rawCity.trim(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                city = rawCity.trim();
            }
        }

        if (city == null) {
            city = isLocal ? "Localhost" : "Direct Access";
        }

        // --- Process Region ---
        String region = null;
        if (rawRegion != null && !rawRegion.isBlank() && !"UNKNOWN".equalsIgnoreCase(rawRegion)) {
            try {
                region = URLDecoder.decode(rawRegion.trim(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                region = rawRegion.trim();
            }
        }

        if (region == null) {
            region = isLocal ? "Local Network" : "Direct";
        }

        return new GeoLocation(country, region, city);
    }

    private static String getFirstHeader(HttpServletRequest request, String... headerNames) {
        for (String name : headerNames) {
            String val = request.getHeader(name);
            if (val != null && !val.isBlank() && !"unknown".equalsIgnoreCase(val.trim())) {
                return val.trim();
            }
        }
        return null;
    }

    private static boolean isIgnoredCountry(String code) {
        String upper = code.trim().toUpperCase();
        return "XX".equals(upper) || "T1".equals(upper) || "UNKNOWN".equals(upper);
    }

    private static boolean isLocalOrPrivateIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return true;
        }
        String clean = ip.trim();
        return "127.0.0.1".equals(clean)
                || "0:0:0:0:0:0:0:1".equals(clean)
                || "::1".equals(clean)
                || "localhost".equalsIgnoreCase(clean)
                || clean.startsWith("10.")
                || clean.startsWith("192.168.")
                || clean.matches("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*");
    }

    // Resolves ISO country from browser Accept-Language header (e.g. "en-IN,en;q=0.9" -> "India", "zh-Hans-CN" -> "China")
    private static String resolveCountryFromAcceptLanguage(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return null;
        }
        String[] parts = acceptLanguage.split(",");
        for (String part : parts) {
            String tag = part.split(";")[0].trim().replace('_', '-');
            if (tag.contains("-")) {
                String[] sub = tag.split("-");
                String candidate = sub[sub.length - 1].toUpperCase();
                if (candidate.length() == 2 && !isIgnoredCountry(candidate)) {
                    String display = Locale.of("", candidate).getDisplayCountry(Locale.ENGLISH);
                    if (display != null && !display.isBlank() && !display.equalsIgnoreCase(candidate)) {
                        return display;
                    }
                }
            }
        }
        return null;
    }

    private static String resolveCountryFromTimezone(String timezoneId) {
        if (timezoneId == null || timezoneId.isBlank()) {
            return null;
        }
        String tz = timezoneId.trim().toLowerCase();

        if (tz.contains("kolkata") || tz.contains("calcutta") || tz.equals("ist") || tz.contains("india")) {
            return "India";
        }
        if (tz.contains("tokyo")) return "Japan";
        if (tz.contains("london")) return "United Kingdom";
        if (tz.contains("paris")) return "France";
        if (tz.contains("berlin")) return "Germany";
        if (tz.contains("dubai")) return "United Arab Emirates";
        if (tz.contains("singapore")) return "Singapore";
        if (tz.contains("sydney") || tz.contains("melbourne")) return "Australia";
        if (tz.contains("toronto") || tz.contains("vancouver")) return "Canada";
        if (tz.contains("new_york") || tz.contains("los_angeles") || tz.contains("chicago")) return "United States";
        if (tz.contains("colombo")) return "Sri Lanka";
        if (tz.contains("dhaka")) return "Bangladesh";
        if (tz.contains("karachi")) return "Pakistan";
        if (tz.contains("kathmandu")) return "Nepal";

        return null;
    }
}
