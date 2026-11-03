package com.shopledger.dao;

import com.shopledger.model.StockMovement;
import com.shopledger.util.Db;
import com.shopledger.util.Page;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StockMovementDao {

    private static final String BASE_SELECT =
            "SELECT m.*, p.name AS product_name, p.sku AS product_sku, p.unit AS product_unit, "
                    + "u.display_name AS operator_name FROM stock_movements m "
                    + "JOIN products p ON m.product_id = p.id LEFT JOIN users u ON m.operator_id = u.id ";

    public Page<StockMovement> search(Long productId, String type, LocalDate start, LocalDate end,
                                      int page, int size) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        appendConditions(where, params, productId, type, start, end);
        long total = count(productId, type, start, end);
        List<StockMovement> items = new ArrayList<>();
        String sql = BASE_SELECT + where + " ORDER BY m.occurred_at DESC, m.id DESC LIMIT ? OFFSET ?";
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
            throw new IllegalStateException("查询库存流水失败", e);
        }
        return new Page<>(items, page, size, total);
    }

    public long count(Long productId, String type, LocalDate start, LocalDate end) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        appendConditions(where, params, productId, type, start, end);
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM stock_movements m" + where)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("统计库存流水失败", e);
        }
    }

    public List<StockMovement> recent(int limit) {
        List<StockMovement> list = new ArrayList<>();
        String sql = BASE_SELECT + " ORDER BY m.occurred_at DESC, m.id DESC LIMIT ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("查询库存流水失败", e);
        }
    }

    public void insert(Connection conn, StockMovement movement) throws SQLException {
        String sql = "INSERT INTO stock_movements (product_id, type, quantity, unit_price, note, occurred_at, "
                + "operator_id, transaction_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, movement.getProductId());
            ps.setString(2, movement.getType());
            ps.setInt(3, movement.getQuantity());
            ps.setBigDecimal(4, movement.getUnitPrice());
            ps.setString(5, movement.getNote());
            ps.setDate(6, Date.valueOf(movement.getOccurredAt()));
            if (movement.getOperatorId() == null) {
                ps.setNull(7, java.sql.Types.BIGINT);
            } else {
                ps.setLong(7, movement.getOperatorId());
            }
            if (movement.getTransactionId() == null) {
                ps.setNull(8, java.sql.Types.BIGINT);
            } else {
                ps.setLong(8, movement.getTransactionId());
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    movement.setId(keys.getLong(1));
                }
            }
        }
    }

    /** 返回 [商品名称, 出库数量, 出库金额] 列表。 */
    public List<Object[]> topProductsBySales(LocalDate start, LocalDate end, int limit) {
        String sql = "SELECT p.name, SUM(m.quantity) AS qty, SUM(m.quantity * m.unit_price) AS amount "
                + "FROM stock_movements m JOIN products p ON m.product_id = p.id "
                + "WHERE m.type = 'OUT' AND m.occurred_at BETWEEN ? AND ? "
                + "GROUP BY p.id, p.name ORDER BY amount DESC LIMIT ?";
        List<Object[]> list = new ArrayList<>();
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(start));
            ps.setDate(2, Date.valueOf(end));
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{rs.getString(1), rs.getLong(2), rs.getBigDecimal(3)});
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("统计热销商品失败", e);
        }
    }

    public long quantitySum(String type, LocalDate start, LocalDate end) {
        String sql = "SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE type = ? AND occurred_at BETWEEN ? AND ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("统计库存数量失败", e);
        }
    }

    public BigDecimal amountSum(String type, LocalDate start, LocalDate end) {
        String sql = "SELECT COALESCE(SUM(quantity * unit_price), 0) FROM stock_movements "
                + "WHERE type = ? AND occurred_at BETWEEN ? AND ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("统计库存金额失败", e);
        }
    }

    private void appendConditions(StringBuilder where, List<Object> params, Long productId, String type,
                                  LocalDate start, LocalDate end) {
        if (productId != null && productId > 0) {
            where.append(" AND m.product_id = ?");
            params.add(productId);
        }
        if (type != null && !type.isEmpty()) {
            where.append(" AND m.type = ?");
            params.add(type);
        }
        if (start != null) {
            where.append(" AND m.occurred_at >= ?");
            params.add(Date.valueOf(start));
        }
        if (end != null) {
            where.append(" AND m.occurred_at <= ?");
            params.add(Date.valueOf(end));
        }
    }

    private int bind(PreparedStatement ps, List<Object> params) throws SQLException {
        int index = 1;
        for (Object param : params) {
            if (param instanceof Date) {
                ps.setDate(index++, (Date) param);
            } else if (param instanceof Long) {
                ps.setLong(index++, (Long) param);
            } else {
                ps.setString(index++, String.valueOf(param));
            }
        }
        return index;
    }

    private StockMovement map(ResultSet rs) throws SQLException {
        StockMovement movement = new StockMovement();
        movement.setId(rs.getLong("id"));
        movement.setProductId(rs.getLong("product_id"));
        movement.setProductName(rs.getString("product_name"));
        movement.setProductSku(rs.getString("product_sku"));
        movement.setProductUnit(rs.getString("product_unit"));
        movement.setType(rs.getString("type"));
        movement.setQuantity(rs.getInt("quantity"));
        movement.setUnitPrice(rs.getBigDecimal("unit_price"));
        movement.setNote(rs.getString("note"));
        movement.setOccurredAt(rs.getDate("occurred_at").toLocalDate());
        long operatorId = rs.getLong("operator_id");
        movement.setOperatorId(rs.wasNull() ? null : operatorId);
        movement.setOperatorName(rs.getString("operator_name"));
        long transactionId = rs.getLong("transaction_id");
        movement.setTransactionId(rs.wasNull() ? null : transactionId);
        return movement;
    }
}
