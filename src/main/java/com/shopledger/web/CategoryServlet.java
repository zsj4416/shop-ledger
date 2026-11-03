package com.shopledger.web;

import com.shopledger.service.CategoryService;
import com.shopledger.util.WebUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/categories")
public class CategoryServlet extends BaseServlet {

    private final CategoryService categoryService = new CategoryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("categories", categoryService.listWithUsage());
        render(request, response, "categories", "分类管理", "categories");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = WebUtil.str(request, "action");
        try {
            long id = WebUtil.lng(request, "id", 0);
            if ("delete".equals(action)) {
                categoryService.delete(id);
                WebUtil.flash(request, "success", "分类已删除");
            } else if ("update".equals(action) || (id > 0 && "save".equals(action))) {
                categoryService.update(id, WebUtil.str(request, "name"),
                        WebUtil.str(request, "type"), WebUtil.integer(request, "sortOrder", 0));
                WebUtil.flash(request, "success", "分类已更新");
            } else {
                categoryService.create(WebUtil.str(request, "name"), WebUtil.str(request, "type"),
                        WebUtil.integer(request, "sortOrder", 0));
                WebUtil.flash(request, "success", "分类已创建");
            }
            WebUtil.redirect(response, request, "/categories");
        } catch (RuntimeException e) {
            fail(request, response, "/categories", e);
        }
    }
}
