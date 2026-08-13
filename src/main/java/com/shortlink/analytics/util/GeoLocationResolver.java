package com.shortlink.analytics.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;

// Utility class resolving country, region, and city from deployment proxy headers (Render, Vercel, Cloudflare, Nginx) with local development fallbacks.
public final class GeoLocationResolver {

    public record GeoLocation(String country, String region, String city) {}

    private GeoLocationResolver() {}

    public static GeoLocation resolve(HttpServletRequest request, String ipAddress) {
        String rawCountry = null;
        String rawRegion = null;
        String rawCity = null;

        if (request != null) {
            // 1. Resolve Country Header (Render, Vercel, Cloudflare, Nginx)
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
        }

        boolean isLocal = isLocalOrPrivateIp(ipAddress);

        // --- Process Country ---
        String country = null;
        if (rawCountry != null && !rawCountry.isBlank() && !isIgnoredCountry(rawCountry)) {
            String code = rawCountry.trim().toUpperCase();
            if (code.length() == 2) {
                String display = Locale.of("", code).getDisplayCountry(Locale.ENGLISH);
                country = (display != null && !display.isBlank()) ? display : code;
            } else {
                country = rawCountry.trim();
            }
        }

        if (country == null) {
            if (isLocal) {
                String systemCountry = Locale.getDefault().getDisplayCountry(Locale.ENGLISH);
                country = (systemCountry != null && !systemCountry.isBlank()) ? systemCountry : "Local Development";
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
}
