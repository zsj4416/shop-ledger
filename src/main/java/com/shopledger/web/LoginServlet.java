package com.shopledger.web;

import com.shopledger.model.User;
import com.shopledger.service.AuthService;
import com.shopledger.util.WebUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends BaseServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (currentUser(request) != null) {
            WebUtil.redirect(response, request, "/dashboard");
            return;
        }
        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = WebUtil.str(request, "username");
        String password = request.getParameter("password");
        String target = WebUtil.safeRedirect(request.getParameter("redirect"), "/dashboard");
        try {
            User user = authService.login(username, password);
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession session = request.getSession(true);
            session.setMaxInactiveInterval(60 * 60);
            session.setAttribute("currentUser", user);
            WebUtil.flash(request, "success", "欢迎回来，" + user.getDisplayName() + "！");
            WebUtil.redirect(response, request, target);
        } catch (RuntimeException e) {
            request.setAttribute("loginError", e.getMessage());
            request.setAttribute("username", username);
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
        }
    }
}
