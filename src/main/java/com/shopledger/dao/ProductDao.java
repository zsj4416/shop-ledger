package com.shopledger.dao;

import com.shopledger.model.Product;
import com.shopledger.util.Db;
import com.shopledger.util.Page;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

    public Page<Product> search(String keyword, String category, boolean lowStockOnly, int page, int size) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        appendConditions(where, params, keyword, category, lowStockOnly);

        long total = count(keyword, category, lowStockOnly);
        List<Product> items = new ArrayList<>();
        String sql = "SELECT * FROM products" + where + " ORDER BY status DESC, id DESC LIMIT ? OFFSET ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = bind(ps, params);
            ps.setInt(index++, size);
            ps.setInt(index, (page - 1) * size);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("查询商品失败", e);
        }
        return new Page<>(items, page, size, total);
    }

    public long count(String keyword, String category, boolean lowStockOnly) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        appendConditions(where, params, keyword, category, lowStockOnly);
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM products" + where)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("统计商品失败", e);
        }
    }

    public List<Product> findAllActive() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE status = 1 ORDER BY name";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("查询商品失败", e);
        }
    }

    public List<Product> findLowStock(int limit) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE status = 1 AND stock <= alert_threshold "
                + "ORDER BY (stock - alert_threshold), stock ASC LIMIT ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("查询库存预警失败", e);
        }
    }

    public Product findById(long id) {
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM products WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("查询商品失败", e);
        }
    }

    public Product findBySku(String sku) {
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM products WHERE sku = ?")) {
            ps.setString(1, sku);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("查询商品失败", e);
        }
    }

    public long insert(Product product) {
        String sql = "INSERT INTO products (sku, name, category, unit, cost_price, sale_price, stock, "
                + "alert_threshold, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, product.getSku());
            ps.setString(2, product.getName());
            ps.setString(3, product.getCategory());
            ps.setString(4, product.getUnit());
            ps.setBigDecimal(5, product.getCostPrice());
            ps.setBigDecimal(6, product.getSalePrice());
            ps.setInt(7, product.getStock());
            ps.setInt(8, product.getAlertThreshold());
            ps.setInt(9, product.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0;
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate")) {
                throw new IllegalStateException("商品编码已存在，请更换");
            }
            throw new IllegalStateException("新增商品失败", e);
        }
    }

    public void update(Product product) {
        String sql = "UPDATE products SET sku = ?, name = ?, category = ?, unit = ?, cost_price = ?, "
                + "sale_price = ?, alert_threshold = ?, status = ? WHERE id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, product.getSku());
            ps.setString(2, product.getName());
            ps.setString(3, product.getCategory());
            ps.setString(4, product.getUnit());
            ps.setBigDecimal(5, product.getCostPrice());
            ps.setBigDecimal(6, product.getSalePrice());
            ps.setInt(7, product.getAlertThreshold());
            ps.setInt(8, product.getStatus());
            ps.setLong(9, product.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate")) {
                throw new IllegalStateException("商品编码已存在，请更换");
            }
            throw new IllegalStateException("更新商品失败", e);
        }
    }

    public void updateStock(Connection conn, long productId, int delta) throws SQLException {
        String sql = "UPDATE products SET stock = stock + ?, updated_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setLong(2, productId);
            ps.executeUpdate();
        }
    }

    public void setStock(long productId, int stock) {
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE products SET stock = ?, updated_at = NOW() WHERE id = ?")) {
            ps.setInt(1, stock);
            ps.setLong(2, productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("调整库存失败", e);
        }
    }

    public Product lockById(Connection conn, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM products WHERE id = ? FOR UPDATE")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void delete(long id) {
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM products WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("删除商品失败，可能已存在库存流水", e);
        }
    }

    public long countAll() {
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM products");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException("统计商品失败", e);
        }
    }

    public long countLowStock() {
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM products WHERE status = 1 AND stock <= alert_threshold");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException("统计库存预警失败", e);
        }
    }

    public BigDecimal totalStockValue() {
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COALESCE(SUM(cost_price * stock), 0) FROM products WHERE status = 1");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        } catch (SQLException e) {
            throw new IllegalStateException("统计库存价值失败", e);
        }
    }

    public List<String> listCategories() {
        List<String> list = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT DISTINCT category FROM products WHERE category IS NOT NULL ORDER BY category");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString(1));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("查询商品分类失败", e);
        }
    }

    private void appendConditions(StringBuilder where, List<Object> params, String keyword,
                                  String category, boolean lowStockOnly) {
        if (keyword != null && !keyword.isEmpty()) {
            where.append(" AND (name LIKE ? OR sku LIKE ?)");
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
        }
        if (category != null && !category.isEmpty()) {
            where.append(" AND category = ?");
            params.add(category);
        }
        if (lowStockOnly) {
            where.append(" AND status = 1 AND stock <= alert_threshold");
        }
    }

    private int bind(PreparedStatement ps, List<Object> params) throws SQLException {
        int index = 1;
        for (Object param : params) {
            ps.setString(index++, String.valueOf(param));
        }
        return index;
    }

    private Product map(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setId(rs.getLong("id"));
        product.setSku(rs.getString("sku"));
        product.setName(rs.getString("name"));
        product.setCategory(rs.getString("category"));
        product.setUnit(rs.getString("unit"));
        product.setCostPrice(rs.getBigDecimal("cost_price"));
        product.setSalePrice(rs.getBigDecimal("sale_price"));
        product.setStock(rs.getInt("stock"));
        product.setAlertThreshold(rs.getInt("alert_threshold"));
        product.setStatus(rs.getInt("status"));
        return product;
    }
}
