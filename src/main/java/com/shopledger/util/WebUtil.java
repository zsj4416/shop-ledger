package com.shopledger.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * 请求参数与页面跳转相关的通用方法。
 */
public final class WebUtil {

    private WebUtil() {
    }

    public static String str(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    public static String str(HttpServletRequest request, String name, String defaultValue) {
        String value = str(request, name);
        return value.isEmpty() ? defaultValue : value;
    }

    public static long lng(HttpServletRequest request, String name, long defaultValue) {
        try {
            String value = str(request, name);
            return value.isEmpty() ? defaultValue : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static int integer(HttpServletRequest request, String name, int defaultValue) {
        try {
            String value = str(request, name);
            return value.isEmpty() ? defaultValue : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static BigDecimal decimal(HttpServletRequest request, String name) {
        try {
            String value = str(request, name);
            return value.isEmpty() ? BigDecimal.ZERO : new BigDecimal(value);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    public static LocalDate date(HttpServletRequest request, String name, LocalDate defaultValue) {
        String value = str(request, name);
        if (value.isEmpty()) {
            return defaultValue;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return defaultValue;
        }
    }

    public static void flash(HttpServletRequest request, String type, String message) {
        request.getSession().setAttribute("flashType", type);
        request.getSession().setAttribute("flashMessage", message);
    }

    public static void redirect(HttpServletResponse response, HttpServletRequest request, String path) throws IOException {
        response.sendRedirect(request.getContextPath() + path);
    }

    /** 仅允许站内相对路径，避免开放重定向。 */
    public static String safeRedirect(String target, String fallback) {
        if (target == null || target.isEmpty() || target.contains("://") || target.startsWith("//")) {
            return fallback;
        }
        return target.startsWith("/") ? target : "/" + target;
    }
}
