package com.shopledger.dao;

import com.shopledger.util.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 简单的键值配置表访问。当前用于记录模拟经营数据的最后生成日期，
 * 保证同一天无论刷新多少次、重启多少次都只生成一次。
 */
public class SettingDao {

    public String get(String key) {
        try (Connection conn = Db.getConnection()) {
            return get(conn, key);
        } catch (SQLException e) {
            throw new IllegalStateException("读取系统配置失败", e);
        }
    }

    public String get(Connection conn, String key) throws SQLException {
        String sql = "SELECT setting_value FROM settings WHERE setting_key = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public void put(String key, String value) {
        try (Connection conn = Db.getConnection()) {
            put(conn, key, value);
        } catch (SQLException e) {
            throw new IllegalStateException("保存系统配置失败", e);
        }
    }

    public void put(Connection conn, String key, String value) throws SQLException {
        String sql = "INSERT INTO settings (setting_key, setting_value) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }
}
