package com.shopledger.web;

import com.shopledger.service.DailyActivityService;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 每天第一次请求时补齐当天的模拟经营流水。
 * 只在应用内页面触发，静态资源直接放行；出错只记日志，不阻断用户操作。
 */
public class DailyActivityFilter implements Filter {

    private final DailyActivityService dailyActivityService = new DailyActivityService();

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!path.startsWith("/assets/") && !path.equals("/favicon.ico")) {
            try {
                dailyActivityService.ensureToday();
            } catch (RuntimeException ignored) {
                // 服务内部已记录日志；这里保证页面仍然可以正常打开
            }
        }
        chain.doFilter(req, res);
    }

    @Override
    public void destroy() {
    }
}
