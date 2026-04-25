package com.naijavehicle.api.utils;

import jakarta.servlet.http.HttpServletRequest;

public class GeneralUtils {

    public  static String getIpAddress(HttpServletRequest request){
            // List of headers to check, ordered by most reliable to least reliable
            String[] headerNames = {
                    "CF-Connecting-IP", // Cloudflare (Very reliable)
                    "X-Forwarded-For",   // Standard (Check first element)
                    "X-Real-IP",        // Nginx/Fastly
                    "Proxy-Client-IP",
                    "WL-Proxy-Client-IP",
                    "HTTP_X_FORWARDED_FOR",
                    "HTTP_X_FORWARDED",
                    "HTTP_X_CLUSTER_CLIENT_IP",
                    "HTTP_CLIENT_IP",
                    "HTTP_FORWARDED_FOR",
                    "HTTP_FORWARDED",
                    "HTTP_VIA",
                    "REMOTE_ADDR"
            };

            for (String header : headerNames) {
                String ip = request.getHeader(header);
                if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                    // X-Forwarded-For can contain multiple IPs (Client, Proxy1, Proxy2)
                    // We always want the first one.
                    return ip.contains(",") ? ip.split(",")[0].trim() : ip;
                }
            }

            // If no headers are found, fall back to the direct remote address
            return request.getRemoteAddr();
    }

    public static String getAppInstallationId(HttpServletRequest request) {
        return request.getHeader("AppInstallationId");
    }
}
