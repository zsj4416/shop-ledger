package com.shopledger.service;

import com.shopledger.dao.ProductDao;
import com.shopledger.dao.StockMovementDao;
import com.shopledger.model.Product;
import com.shopledger.util.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

public class ProductService {

    private final ProductDao productDao = new ProductDao();
    private final StockMovementDao movementDao = new StockMovementDao();

    public Page<Product> search(String keyword, String category, boolean lowStockOnly, int page, int size) {
        return productDao.search(keyword, category, lowStockOnly, Math.max(1, page), size);
    }

    public List<Product> findLowStock(int limit) {
        return productDao.findLowStock(limit);
    }

    public List<Product> findAllActive() {
        return productDao.findAllActive();
    }

    public List<String> listCategories() {
        return productDao.listCategories();
    }

    public Product findById(long id) {
        return productDao.findById(id);
    }

    public long create(Product product) {
        normalize(product);
        validate(product, true);
        return productDao.insert(product);
    }

    public void update(Product product) {
        normalize(product);
        Product existing = productDao.findById(product.getId() == null ? 0 : product.getId());
        if (existing == null) {
            throw new IllegalArgumentException("商品不存在或已被删除");
        }
        validate(product, false);
        productDao.update(product);
    }

    public void toggleStatus(long id) {
        Product product = productDao.findById(id);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        product.setStatus(product.getStatus() == 1 ? 0 : 1);
        productDao.update(product);
    }

    public void delete(long id) {
        long movements = movementDao.count(id, null, null, null);
        if (movements > 0) {
            throw new IllegalStateException("该商品已有 " + movements + " 条库存流水，不能删除，可将商品设为停用");
        }
        productDao.delete(id);
    }

    public String generateSku() {
        return "SP" + System.currentTimeMillis() % 100000000L;
    }

    private void normalize(Product product) {
        if (product.getSku() == null || product.getSku().trim().isEmpty()) {
            product.setSku(generateSku());
        } else {
            product.setSku(product.getSku().trim().toUpperCase(Locale.ROOT));
        }
        if (product.getUnit() == null || product.getUnit().trim().isEmpty()) {
            product.setUnit("件");
        }
        if (product.getStock() < 0) {
            product.setStock(0);
        }
    }

    private void validate(Product product, boolean creating) {
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("请输入商品名称");
        }
        if (product.getName().length() > 100) {
            throw new IllegalArgumentException("商品名称不能超过 100 个字符");
        }
        if (product.getCategory() == null || product.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("请输入商品分类");
        }
        if (product.getCostPrice() == null || product.getCostPrice().compareTo(BigDecimal.ZERO) < 0
                || product.getSalePrice() == null || product.getSalePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("进价和售价不能为负数");
        }
        if (product.getAlertThreshold() < 0) {
            throw new IllegalArgumentException("预警阈值不能为负数");
        }
        if (creating && product.getStock() < 0) {
            throw new IllegalArgumentException("初始库存不能为负数");
        }
        Product sameSku = productDao.findBySku(product.getSku());
        if (sameSku != null && !sameSku.getId().equals(product.getId())) {
            throw new IllegalArgumentException("商品编码 " + product.getSku() + " 已存在");
        }
    }
}
