package com.shopledger.web;

import com.shopledger.model.Reminder;
import com.shopledger.service.ProductService;
import com.shopledger.service.ReminderService;
import com.shopledger.util.WebUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;

@WebServlet("/reminders")
public class ReminderServlet extends BaseServlet {

    private final ReminderService reminderService = new ReminderService();
    private final ProductService productService = new ProductService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        reminderService.reconcileStockReminders();
        String status = WebUtil.str(request, "status", "pending");
        if (!"pending".equals(status) && !"done".equals(status) && !"all".equals(status)) {
            status = "pending";
        }
        request.setAttribute("reminders", reminderService.searchWithExpenses(status));
        request.setAttribute("filterStatus", status);
        request.setAttribute("lowStockProducts", productService.findLowStock(8));
        request.setAttribute("today", LocalDate.now());
        render(request, response, "reminders", "提醒中心", "reminders");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String back = WebUtil.safeRedirect(WebUtil.str(request, "back"), "/reminders");
        String action = WebUtil.str(request, "action");
        try {
            long id = WebUtil.lng(request, "id", 0);

            if ("delete".equals(action)) {
                reminderService.delete(id);
                WebUtil.flash(request, "success", "提醒已删除");
                WebUtil.redirect(response, request, back);
                return;
            }
            if ("toggle".equals(action)) {
                boolean done = "1".equals(WebUtil.str(request, "done"));
                Reminder reminder = reminderService.findById(id);
                reminderService.toggle(id, done);
                String message = done ? "已标记为完成" : "已恢复为待办";
                if (done && reminder != null) {
                    reminderService.attachRelatedExpense(reminder);
                    if (reminder.getRelatedCategoryName() != null) {
                        message += "；本月" + reminder.getRelatedCategoryName() + "支出已记在 "
                                + reminder.getRelatedDate() + "（¥"
                                + String.format("%,.2f", reminder.getRelatedAmount()) + "）";
                    }
                }
                WebUtil.flash(request, "success", message);
                WebUtil.redirect(response, request, back);
                return;
            }

            Reminder reminder = new Reminder();
            reminder.setId(id > 0 ? id : null);
            reminder.setTitle(WebUtil.str(request, "title"));
            reminder.setContent(WebUtil.str(request, "content"));
            reminder.setDueDate(WebUtil.date(request, "dueDate", LocalDate.now()));
            reminder.setPriority(WebUtil.str(request, "priority", "MEDIUM"));
            if (id > 0) {
                Reminder existing = reminderService.findById(id);
                reminder.setDone(existing != null && existing.isDone());
                reminderService.update(reminder);
                WebUtil.flash(request, "success", "提醒已更新");
            } else {
                reminder.setDone(false);
                reminderService.create(reminder);
                WebUtil.flash(request, "success", "提醒已创建");
            }
            WebUtil.redirect(response, request, back);
        } catch (RuntimeException e) {
            fail(request, response, back, e);
        }
    }
}
