package com.shopledger.dao;

import com.shopledger.model.Reminder;
import com.shopledger.util.Db;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReminderDao {

    public List<Reminder> search(String status) {
        StringBuilder sql = new StringBuilder("SELECT * FROM reminders WHERE 1=1");
        if ("pending".equals(status)) {
            sql.append(" AND done = 0");
        } else if ("done".equals(status)) {
            sql.append(" AND done = 1");
        }
        sql.append(" ORDER BY done, due_date, id DESC");
        List<Reminder> list = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("查询提醒失败", e);
        }
    }

    public List<Reminder> findUpcoming(int days, int limit) {
        String sql = "SELECT * FROM reminders WHERE done = 0 AND due_date <= ? ORDER BY due_date, id LIMIT ?";
        List<Reminder> list = new ArrayList<>();
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(LocalDate.now().plusDays(days)));
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("查询待办提醒失败", e);
        }
    }

    public Reminder findById(long id) {
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM reminders WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("查询提醒失败", e);
        }
    }

    public long insert(Reminder reminder) {
        String sql = "INSERT INTO reminders (title, content, due_date, priority, done) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, reminder.getTitle());
            ps.setString(2, reminder.getContent());
            ps.setDate(3, Date.valueOf(reminder.getDueDate()));
            ps.setString(4, reminder.getPriority());
            ps.setBoolean(5, reminder.isDone());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("新增提醒失败", e);
        }
    }

    public void update(Reminder reminder) {
        String sql = "UPDATE reminders SET title = ?, content = ?, due_date = ?, priority = ?, done = ? WHERE id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reminder.getTitle());
            ps.setString(2, reminder.getContent());
            ps.setDate(3, Date.valueOf(reminder.getDueDate()));
            ps.setString(4, reminder.getPriority());
            ps.setBoolean(5, reminder.isDone());
            ps.setLong(6, reminder.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("更新提醒失败", e);
        }
    }

    public void toggleDone(long id, boolean done) {
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE reminders SET done = ? WHERE id = ?")) {
            ps.setBoolean(1, done);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("更新提醒状态失败", e);
        }
    }

    public void delete(long id) {
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM reminders WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("删除提醒失败", e);
        }
    }

    /**
     * 库存充足时，自动完成标题以“补货”开头的待办，避免库存页已无预警、
     * 待办里却还挂着一条过期补货提醒。
     */
    public int completeStockReminders() {
        String sql = "UPDATE reminders SET done = 1 WHERE done = 0 AND title LIKE '补货%'";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("更新补货提醒失败", e);
        }
    }

    public long countPending() {
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM reminders WHERE done = 0");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException("统计提醒失败", e);
        }
    }

    public long countOverdue() {
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM reminders WHERE done = 0 AND due_date < CURDATE()");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException("统计逾期提醒失败", e);
        }
    }

    private Reminder map(ResultSet rs) throws SQLException {
        Reminder reminder = new Reminder();
        reminder.setId(rs.getLong("id"));
        reminder.setTitle(rs.getString("title"));
        reminder.setContent(rs.getString("content"));
        reminder.setDueDate(rs.getDate("due_date").toLocalDate());
        reminder.setPriority(rs.getString("priority"));
        reminder.setDone(rs.getBoolean("done"));
        return reminder;
    }
}
