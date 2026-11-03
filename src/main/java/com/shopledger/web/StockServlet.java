package com.shopledger.web;

import com.shopledger.model.Product;
import com.shopledger.model.StockMovement;
import com.shopledger.service.ProductService;
import com.shopledger.service.StockService;
import com.shopledger.util.Page;
import com.shopledger.util.WebUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

@WebServlet("/stock")
public class StockServlet extends BaseServlet {

    private final StockService stockService = new StockService();
    private final ProductService productService = new ProductService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LocalDate today = LocalDate.now();
        long productId = WebUtil.lng(request, "productId", 0);
        String type = WebUtil.str(request, "type");
        if (!"IN".equals(type) && !"OUT".equals(type) && !"ADJUST".equals(type)) {
            type = "";
        }
        LocalDate start = WebUtil.date(request, "start", today.withDayOfMonth(1));
        LocalDate end = WebUtil.date(request, "end", today);
        int page = WebUtil.integer(request, "page", 1);

        Page<StockMovement> pageData = stockService.search(productId > 0 ? productId : null,
                type, start, end, page, 12);

        request.setAttribute("pageData", pageData);
        request.setAttribute("products", productService.findAllActive());
        request.setAttribute("selectedProduct", productId > 0 ? productService.findById(productId) : null);
        request.setAttribute("summary", stockService.summary(start, end));
        request.setAttribute("filterProductId", productId);
        request.setAttribute("filterType", type);
        request.setAttribute("filterStart", start);
        request.setAttribute("filterEnd", end);
        request.setAttribute("openModal", WebUtil.str(request, "open"));
        request.setAttribute("today", today);
        render(request, response, "stock", "库存流水", "stock");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String back = WebUtil.safeRedirect(WebUtil.str(request, "back"), "/stock");
        try {
            long productId = WebUtil.lng(request, "productId", 0);
            String type = WebUtil.str(request, "type");
            int quantity = WebUtil.integer(request, "quantity", 0);
            String priceRaw = WebUtil.str(request, "unitPrice");
            BigDecimal unitPrice = priceRaw.isEmpty() ? null : WebUtil.decimal(request, "unitPrice");
            String note = WebUtil.str(request, "note");
            LocalDate occurredAt = WebUtil.date(request, "occurredAt", LocalDate.now());
            boolean syncIncome = "1".equals(WebUtil.str(request, "syncIncome"))
                    || "on".equals(WebUtil.str(request, "syncIncome"));
            boolean syncExpense = "1".equals(WebUtil.str(request, "syncExpense"))
                    || "on".equals(WebUtil.str(request, "syncExpense"));

            StockMovement movement = stockService.record(productId, type, quantity, unitPrice, note,
                    occurredAt, currentUser(request).getId(), syncIncome, syncExpense);

            String label = "IN".equals(type) ? "入库" : "OUT".equals(type) ? "出库" : "盘点";
            String extra = "IN".equals(type) && syncExpense ? "，已记一笔进货支出" : "";
            WebUtil.flash(request, "success", label + "成功：" + movement.getProductName()
                    + " " + movement.getTypeText() + " " + Math.abs(movement.getSignedQuantity())
                    + movement.getProductUnit() + extra);
            WebUtil.redirect(response, request, back);
        } catch (RuntimeException e) {
            fail(request, response, back, e);
        }
    }
}
