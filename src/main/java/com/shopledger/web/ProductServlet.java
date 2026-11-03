package com.shopledger.web;

import com.shopledger.model.Product;
import com.shopledger.service.ProductService;
import com.shopledger.service.StockService;
import com.shopledger.util.Page;
import com.shopledger.util.WebUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;

@WebServlet("/products")
public class ProductServlet extends BaseServlet {

    private final ProductService productService = new ProductService();
    private final StockService stockService = new StockService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String keyword = WebUtil.str(request, "keyword");
        String category = WebUtil.str(request, "category");
        boolean lowStockOnly = "1".equals(WebUtil.str(request, "low"));
        int page = WebUtil.integer(request, "page", 1);

        Page<Product> pageData = productService.search(keyword, category, lowStockOnly, page, 10);
        request.setAttribute("pageData", pageData);
        request.setAttribute("filterKeyword", keyword);
        request.setAttribute("filterCategory", category);
        request.setAttribute("lowStockOnly", lowStockOnly);
        request.setAttribute("productCategories", productService.listCategories());
        request.setAttribute("lowStockCount", productService.findLowStock(100).size());
        render(request, response, "products", "商品档案", "products");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String back = WebUtil.safeRedirect(WebUtil.str(request, "back"), "/products");
        String action = WebUtil.str(request, "action");
        try {
            long id = WebUtil.lng(request, "id", 0);

            if ("delete".equals(action)) {
                productService.delete(id);
                WebUtil.flash(request, "success", "商品已删除");
                WebUtil.redirect(response, request, back);
                return;
            }
            if ("toggle".equals(action)) {
                productService.toggleStatus(id);
                WebUtil.flash(request, "success", "商品状态已更新");
                WebUtil.redirect(response, request, back);
                return;
            }

            Product product = new Product();
            product.setId(id > 0 ? id : null);
            product.setSku(WebUtil.str(request, "sku"));
            product.setName(WebUtil.str(request, "name"));
            product.setCategory(WebUtil.str(request, "category"));
            product.setUnit(WebUtil.str(request, "unit", "件"));
            product.setCostPrice(WebUtil.decimal(request, "costPrice"));
            product.setSalePrice(WebUtil.decimal(request, "salePrice"));
            product.setAlertThreshold(WebUtil.integer(request, "alertThreshold", 5));

            if (id > 0) {
                Product existing = productService.findById(id);
                if (existing == null) {
                    throw new IllegalArgumentException("商品不存在或已被删除");
                }
                product.setStock(existing.getStock());
                product.setStatus(existing.getStatus());
                productService.update(product);
                WebUtil.flash(request, "success", "商品信息已更新");
            } else {
                int initialStock = WebUtil.integer(request, "stock", 0);
                product.setStock(0);
                product.setStatus(1);
                long newId = productService.create(product);
                if (initialStock > 0) {
                    stockService.record(newId, "IN", initialStock, product.getCostPrice(), "期初库存",
                            LocalDate.now(), currentUser(request).getId(), false, false);
                }
                WebUtil.flash(request, "success", "商品已创建" + (initialStock > 0 ? "，并已录入期初库存" : ""));
            }
            WebUtil.redirect(response, request, back);
        } catch (RuntimeException e) {
            fail(request, response, back, e);
        }
    }
}
