package com.shopledger.web;

import com.shopledger.model.Category;
import com.shopledger.model.Transaction;
import com.shopledger.service.CategoryService;
import com.shopledger.service.TransactionService;
import com.shopledger.util.Page;
import com.shopledger.util.WebUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@WebServlet("/transactions")
public class TransactionServlet extends BaseServlet {

    private final TransactionService transactionService = new TransactionService();
    private final CategoryService categoryService = new CategoryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LocalDate today = LocalDate.now();
        String type = WebUtil.str(request, "type");
        if (!"INCOME".equals(type) && !"EXPENSE".equals(type)) {
            type = "";
        }
        long categoryId = WebUtil.lng(request, "categoryId", 0);
        LocalDate start = WebUtil.date(request, "start", today.withDayOfMonth(1));
        LocalDate end = WebUtil.date(request, "end", today);
        String keyword = WebUtil.str(request, "keyword");
        int page = WebUtil.integer(request, "page", 1);

        Page<Transaction> pageData = transactionService.search(type, categoryId > 0 ? categoryId : null,
                start, end, keyword, page, 10);

        List<Category> categories = categoryService.findAll();
        request.setAttribute("pageData", pageData);
        request.setAttribute("categories", categories);
        request.setAttribute("filterType", type);
        request.setAttribute("filterCategoryId", categoryId);
        request.setAttribute("filterStart", start);
        request.setAttribute("filterEnd", end);
        request.setAttribute("filterKeyword", keyword);
        request.setAttribute("totalIncome", transactionService.sum("INCOME", start, end));
        request.setAttribute("totalExpense", transactionService.sum("EXPENSE", start, end));
        request.setAttribute("today", today);
        request.setAttribute("incomeCategories", categoryService.findByType("INCOME"));
        request.setAttribute("expenseCategories", categoryService.findByType("EXPENSE"));
        render(request, response, "transactions", "收支记账", "transactions");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String back = WebUtil.safeRedirect(WebUtil.str(request, "back"), "/transactions");
        String action = WebUtil.str(request, "action");
        try {
            if ("delete".equals(action)) {
                transactionService.delete(WebUtil.lng(request, "id", 0));
                WebUtil.flash(request, "success", "该笔收支已删除");
                WebUtil.redirect(response, request, back);
                return;
            }

            long id = WebUtil.lng(request, "id", 0);
            String type = WebUtil.str(request, "type");
            if (!"INCOME".equals(type) && !"EXPENSE".equals(type)) {
                throw new IllegalArgumentException("请选择收支类型");
            }
            Transaction txn = new Transaction();
            txn.setId(id > 0 ? id : null);
            txn.setType(type);
            txn.setCategoryId(WebUtil.lng(request, "categoryId", 0));
            txn.setAmount(WebUtil.decimal(request, "amount"));
            txn.setTxnDate(WebUtil.date(request, "txnDate", LocalDate.now()));
            txn.setPaymentMethod(WebUtil.str(request, "paymentMethod", "现金"));
            txn.setRemark(WebUtil.str(request, "remark"));
            txn.setCreatedBy(currentUser(request).getId());

            if (id > 0) {
                transactionService.update(txn);
                WebUtil.flash(request, "success", "收支记录已更新");
            } else {
                transactionService.save(txn);
                WebUtil.flash(request, "success", "已记一笔" + ("INCOME".equals(type) ? "收入" : "支出")
                        + " ¥" + txn.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString());
            }
            WebUtil.redirect(response, request, back);
        } catch (RuntimeException e) {
            fail(request, response, back, e);
        }
    }
}
