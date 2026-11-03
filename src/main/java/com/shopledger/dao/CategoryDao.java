package com.shopledger.dao;

import com.shopledger.model.Category;
import com.shopledger.util.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CategoryDao {

    public List<Category> findAll() {
        String sql = "SELECT * FROM categories ORDER BY type DESC, sort_order, id";
        List<Category> list = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("查询分类失败", e);
        }
    }

    public List<Category> findByType(String type) {
        String sql = "SELECT * FROM categories WHERE type = ? ORDER BY sort_order, id";
        List<Category> list = new ArrayList<>();
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("查询分类失败", e);
        }
    }

    public Category findById(long id) {
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM categories WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("查询分类失败", e);
        }
    }

    public long insert(Category category) {
        String sql = "INSERT INTO categories (name, type, sort_order) VALUES (?, ?, ?)";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getType());
            ps.setInt(3, category.getSortOrder());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0;
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate")) {
                throw new IllegalStateException("同类型下已存在同名分类");
            }
            throw new IllegalStateException("新增分类失败", e);
        }
    }

    public void update(Category category) {
        String sql = "UPDATE categories SET name = ?, type = ?, sort_order = ? WHERE id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getType());
            ps.setInt(3, category.getSortOrder());
            ps.setLong(4, category.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate")) {
                throw new IllegalStateException("同类型下已存在同名分类");
            }
            throw new IllegalStateException("更新分类失败", e);
        }
    }

    public void delete(long id) {
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM categories WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("删除分类失败", e);
        }
    }

    public long countTransactions(long categoryId) {
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM transactions WHERE category_id = ?")) {
            ps.setLong(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("统计分类使用情况失败", e);
        }
    }

    private Category map(ResultSet rs) throws SQLException {
        Category category = new Category();
        category.setId(rs.getLong("id"));
        category.setName(rs.getString("name"));
        category.setType(rs.getString("type"));
        category.setSortOrder(rs.getInt("sort_order"));
        return category;
    }
}
