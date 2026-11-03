package com.shopledger.service;

import com.shopledger.dao.CategoryDao;
import com.shopledger.dao.ProductDao;
import com.shopledger.dao.ReminderDao;
import com.shopledger.dao.TransactionDao;
import com.shopledger.model.Category;
import com.shopledger.model.Reminder;
import com.shopledger.model.Transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReminderService {

    private static final Logger LOG = Logger.getLogger(ReminderService.class.getName());

    private final ReminderDao reminderDao = new ReminderDao();
    private final ProductDao productDao = new ProductDao();
    private final CategoryDao categoryDao = new CategoryDao();
    private final TransactionDao transactionDao = new TransactionDao();

    public List<Reminder> search(String status) {
        return reminderDao.search(status);
    }

    /**
     * 提醒列表额外关联本月同分类的最近一笔支出。
     * 例如“缴纳本月房租”会自动关联 10-01 已记录的 1,400.00 元房租。
     */
    public List<Reminder> searchWithExpenses(String status) {
        List<Reminder> reminders = reminderDao.search(status);
        for (Reminder reminder : reminders) {
            attachRelatedExpense(reminder);
        }
        return reminders;
    }

    /**
     * 标题里包含某个支出分类名时，找到本月该分类最近一笔支出并挂到提醒上。
     */
    public Reminder attachRelatedExpense(Reminder reminder) {
        if (reminder == null || reminder.getTitle() == null) {
            return reminder;
        }
        String title = reminder.getTitle();
        Category matched = null;
        for (Category category : categoryDao.findByType("EXPENSE")) {
            String name = category.getName();
            if (name != null && !name.isEmpty() && title.contains(name)
                    && (matched == null || name.length() > matched.getName().length())) {
                matched = category;
            }
        }
        if (matched == null) {
            return reminder;
        }
        LocalDate today = LocalDate.now();
        Transaction txn = transactionDao.findLatestExpenseByCategory(
                matched.getId(), today.withDayOfMonth(1), today);
        if (txn != null) {
            reminder.setRelatedCategoryId(matched.getId());
            reminder.setRelatedCategoryName(matched.getName());
            reminder.setRelatedAmount(txn.getAmount());
            reminder.setRelatedDate(txn.getTxnDate());
        }
        return reminder;
    }

    /**
     * 所有商品库存都高于预警线时，把遗留的“补货”待办自动标记完成。
     * 这样库存页显示“库存都很充足”时，待办里不会还留下无法处理的补货预警。
     */
    public void reconcileStockReminders() {
        try {
            if (productDao.countLowStock() == 0) {
                int closed = reminderDao.completeStockReminders();
                if (closed > 0) {
                    LOG.info("库存已恢复充足，自动完成 " + closed + " 条补货提醒");
                }
            }
        } catch (RuntimeException e) {
            // 自动清理只是辅助逻辑，不能影响库存入账和页面渲染
            LOG.log(Level.WARNING, "自动完成补货提醒失败", e);
        }
    }

    public List<Reminder> upcoming(int days) {
        return reminderDao.findUpcoming(days, 20);
    }

    public Reminder findById(long id) {
        return reminderDao.findById(id);
    }

    public long create(Reminder reminder) {
        validate(reminder);
        return reminderDao.insert(reminder);
    }

    public void update(Reminder reminder) {
        validate(reminder);
        reminderDao.update(reminder);
    }

    public void toggle(long id, boolean done) {
        reminderDao.toggleDone(id, done);
    }

    public void delete(long id) {
        reminderDao.delete(id);
    }

    private void validate(Reminder reminder) {
        if (reminder.getTitle() == null || reminder.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("请输入提醒标题");
        }
        if (reminder.getTitle().length() > 100) {
            throw new IllegalArgumentException("提醒标题不能超过 100 个字符");
        }
        if (reminder.getDueDate() == null) {
            throw new IllegalArgumentException("请选择提醒日期");
        }
        if (reminder.getContent() != null && reminder.getContent().length() > 500) {
            throw new IllegalArgumentException("提醒内容不能超过 500 个字符");
        }
        if (reminder.getPriority() == null || reminder.getPriority().isEmpty()) {
            reminder.setPriority("MEDIUM");
        }
        if (reminder.getDueDate().isBefore(LocalDate.of(2000, 1, 1))) {
            throw new IllegalArgumentException("提醒日期不合法");
        }
    }
}
