package com.shopledger.dao;

import com.shopledger.model.Transaction;
import com.shopledger.util.Db;
import com.shopledger.util.Page;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransactionDao {

    private static final String BASE_SELECT =
            "SELECT t.*, c.name AS category_name, u.display_name AS created_by_name "
                    + "FROM transactions t JOIN categories c ON t.category_id = c.id "
                    + "LEFT JOIN users u ON t.created_by = u.id ";

    public Page<Transaction> search(String type, Long categoryId, LocalDate start, LocalDate end,
                                    String keyword, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        appendConditions(where, params, type, categoryId, start, end, keyword);

        long total = count(type, categoryId, start, end, keyword);
        List<Transaction> items = new ArrayList<>();
        String sql = BASE_SELECT + where + " ORDER BY t.txn_date DESC, t.id DESC LIMIT ? OFFSET ?";
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
            throw new IllegalStateException("查询收支记录失败", e);
        }
        return new Page<>(items, page, size, total);
    }

    public long count(String type, Long categoryId, LocalDate start, LocalDate end, String keyword) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        appendConditions(where, params, type, categoryId, start, end, keyword);
        String sql = "SELECT COUNT(*) FROM transactions t JOIN categories c ON t.category_id = c.id " + where;
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("统计收支记录失败", e);
        }
    }

    /** 某天是否已有收支记录，用于每日模拟流水去重。 */
    public long countByDate(LocalDate date) {
        String sql = "SELECT COUNT(*) FROM transactions WHERE txn_date = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("统计当日收支失败", e);
        }
    }

    public Transaction findById(long id) {
        String sql = BASE_SELECT + " WHERE t.id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("查询收支记录失败", e);
        }
    }

    /**
     * 找出某天由“每日模拟”生成、但还没有对应库存流水的销售收入。
     * 用于升级旧数据：早期版本只写了收支，没有扣库存。
     */
    public List<Transaction> findUnlinkedSimulatedSales(LocalDate day) {
        String sql = BASE_SELECT + " LEFT JOIN stock_movements m ON m.transaction_id = t.id"
                + " WHERE t.type = 'INCOME' AND t.txn_date = ? AND t.remark LIKE '零售：%'"
                + " AND m.id IS NULL ORDER BY t.id";
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(day));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("查询待补库存的模拟销售失败", e);
        }
    }

    /**
     * 查询某分类在日期范围内最近的一笔支出，用于把提醒和已经记好的支出关联起来。
     */
    public Transaction findLatestExpenseByCategory(long categoryId, LocalDate start, LocalDate end) {
        String sql = BASE_SELECT
                + " WHERE t.type = 'EXPENSE' AND t.category_id = ? AND t.txn_date BETWEEN ? AND ?"
                + " ORDER BY t.txn_date DESC, t.id DESC LIMIT 1";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, categoryId);
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("查询关联支出失败", e);
        }
    }

    public List<Transaction> recent(int limit) {
        String sql = BASE_SELECT + " ORDER BY t.txn_date DESC, t.id DESC LIMIT ?";
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("查询最近收支失败", e);
        }
    }

    public long insert(Transaction txn) {
        try (Connection conn = Db.getConnection()) {
            return insert(conn, txn);
        } catch (SQLException e) {
            throw new IllegalStateException("新增收支记录失败", e);
        }
    }

    public long insert(Connection conn, Transaction txn) throws SQLException {
        String sql = "INSERT INTO transactions (type, category_id, amount, txn_date, payment_method, remark, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, txn.getType());
            ps.setLong(2, txn.getCategoryId());
            ps.setBigDecimal(3, txn.getAmount());
            ps.setDate(4, Date.valueOf(txn.getTxnDate()));
            ps.setString(5, txn.getPaymentMethod());
            ps.setString(6, txn.getRemark());
            if (txn.getCreatedBy() == null) {
                ps.setNull(7, java.sql.Types.BIGINT);
            } else {
                ps.setLong(7, txn.getCreatedBy());
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0;
            }
        }
    }

    public void update(Transaction txn) {
        String sql = "UPDATE transactions SET type = ?, category_id = ?, amount = ?, txn_date = ?, "
                + "payment_method = ?, remark = ? WHERE id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, txn.getType());
            ps.setLong(2, txn.getCategoryId());
            ps.setBigDecimal(3, txn.getAmount());
            ps.setDate(4, Date.valueOf(txn.getTxnDate()));
            ps.setString(5, txn.getPaymentMethod());
            ps.setString(6, txn.getRemark());
            ps.setLong(7, txn.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("更新收支记录失败", e);
        }
    }

    public void delete(long id) {
        Db.inTransaction(conn -> {
            // 删除收支记录时解除库存流水对它的引用，避免出现指向不存在记录的链接
            try (PreparedStatement unlink = conn.prepareStatement(
                    "UPDATE stock_movements SET transaction_id = NULL WHERE transaction_id = ?")) {
                unlink.setLong(1, id);
                unlink.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM transactions WHERE id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public BigDecimal sum(String type, LocalDate start, LocalDate end) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = ? AND txn_date BETWEEN ? AND ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("统计金额失败", e);
        }
    }

    /** 返回 [日期, 收入, 支出] 列表。 */
    public List<Object[]> dailyTotals(LocalDate start, LocalDate end) {
        String sql = "SELECT txn_date, "
                + "COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS income, "
                + "COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS expense "
                + "FROM transactions WHERE txn_date BETWEEN ? AND ? GROUP BY txn_date ORDER BY txn_date";
        List<Object[]> list = new ArrayList<>();
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(start));
            ps.setDate(2, Date.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{rs.getDate(1).toLocalDate(), rs.getBigDecimal(2), rs.getBigDecimal(3)});
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("统计每日收支失败", e);
        }
    }

    /** 返回 [月份(yyyy-MM), 收入, 支出] 列表。 */
    public List<Object[]> monthlyTotals(LocalDate start, LocalDate end) {
        String sql = "SELECT DATE_FORMAT(txn_date, '%Y-%m') AS month, "
                + "COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS income, "
                + "COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS expense "
                + "FROM transactions WHERE txn_date BETWEEN ? AND ? GROUP BY month ORDER BY month";
        List<Object[]> list = new ArrayList<>();
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(start));
            ps.setDate(2, Date.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{rs.getString(1), rs.getBigDecimal(2), rs.getBigDecimal(3)});
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("统计每月收支失败", e);
        }
    }

    /** 返回 [分类名称, 金额] 列表。 */
    public List<Object[]> categoryTotals(String type, LocalDate start, LocalDate end) {
        String sql = "SELECT c.name, COALESCE(SUM(t.amount), 0) AS total "
                + "FROM transactions t JOIN categories c ON t.category_id = c.id "
                + "WHERE t.type = ? AND t.txn_date BETWEEN ? AND ? "
                + "GROUP BY c.id, c.name ORDER BY total DESC";
        List<Object[]> list = new ArrayList<>();
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{rs.getString(1), rs.getBigDecimal(2)});
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("统计分类金额失败", e);
        }
    }

    private void appendConditions(StringBuilder where, List<Object> params, String type, Long categoryId,
                                  LocalDate start, LocalDate end, String keyword) {
        if (type != null && !type.isEmpty()) {
            where.append(" AND t.type = ?");
            params.add(type);
        }
        if (categoryId != null && categoryId > 0) {
            where.append(" AND t.category_id = ?");
            params.add(categoryId);
        }
        if (start != null) {
            where.append(" AND t.txn_date >= ?");
            params.add(Date.valueOf(start));
        }
        if (end != null) {
            where.append(" AND t.txn_date <= ?");
            params.add(Date.valueOf(end));
        }
        if (keyword != null && !keyword.isEmpty()) {
            where.append(" AND (t.remark LIKE ? OR c.name LIKE ?)");
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
        }
    }

    private int bind(PreparedStatement ps, List<Object> params) throws SQLException {
        int index = 1;
        for (Object param : params) {
            if (param instanceof Date) {
                ps.setDate(index++, (Date) param);
            } else if (param instanceof BigDecimal) {
                ps.setBigDecimal(index++, (BigDecimal) param);
            } else if (param instanceof Integer) {
                ps.setInt(index++, (Integer) param);
            } else if (param instanceof Long) {
                ps.setLong(index++, (Long) param);
            } else {
                ps.setString(index++, String.valueOf(param));
            }
        }
        return index;
    }

    private Transaction map(ResultSet rs) throws SQLException {
        Transaction txn = new Transaction();
        txn.setId(rs.getLong("id"));
        txn.setType(rs.getString("type"));
        txn.setCategoryId(rs.getLong("category_id"));
        txn.setCategoryName(rs.getString("category_name"));
        txn.setAmount(rs.getBigDecimal("amount"));
        txn.setTxnDate(rs.getDate("txn_date").toLocalDate());
        txn.setPaymentMethod(rs.getString("payment_method"));
        txn.setRemark(rs.getString("remark"));
        long createdBy = rs.getLong("created_by");
        txn.setCreatedBy(rs.wasNull() ? null : createdBy);
        txn.setCreatedByName(rs.getString("created_by_name"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            txn.setCreatedAt(createdAt.toLocalDateTime());
        }
        return txn;
    }
}
