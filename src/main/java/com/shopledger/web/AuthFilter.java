package com.shopledger.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;

public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (path.equals("/login") || path.startsWith("/assets/") || path.equals("/favicon.ico")) {
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("currentUser") != null) {
            chain.doFilter(req, res);
            return;
        }

        // 只记录应用内路径，登录成功后由 WebUtil.redirect 统一补上 contextPath
        String target = path;
        if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
            target += "?" + request.getQueryString();
        }
        response.sendRedirect(request.getContextPath() + "/login?redirect="
                + URLEncoder.encode(target, "UTF-8"));
    }

    @Override
    public void destroy() {
    }
}
