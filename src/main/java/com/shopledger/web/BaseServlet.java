package com.shopledger.web;

import com.shopledger.model.User;
import com.shopledger.util.WebUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class BaseServlet extends HttpServlet {

    protected final Logger log = Logger.getLogger(getClass().getName());

    protected User currentUser(HttpServletRequest request) {
        Object user = request.getSession().getAttribute("currentUser");
        return user instanceof User ? (User) user : null;
    }

    protected void render(HttpServletRequest request, HttpServletResponse response, String view, String title,
                          String active) throws ServletException, IOException {
        request.setAttribute("pageTitle", title);
        request.setAttribute("activeNav", active);
        request.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(request, response);
    }

    protected void fail(HttpServletRequest request, HttpServletResponse response, String back, Exception e)
            throws IOException {
        String message = e.getMessage() == null || e.getMessage().isEmpty() ? "操作失败，请稍后重试" : e.getMessage();
        log.log(Level.WARNING, message, e);
        WebUtil.flash(request, "danger", message);
        WebUtil.redirect(response, request, WebUtil.safeRedirect(back, "/dashboard"));
    }
}
