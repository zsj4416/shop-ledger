package com.shopledger.service;

import com.shopledger.dao.ProductDao;
import com.shopledger.dao.ReminderDao;
import com.shopledger.dao.StockMovementDao;
import com.shopledger.dao.TransactionDao;
import com.shopledger.model.Product;
import com.shopledger.model.Reminder;
import com.shopledger.model.Transaction;
import com.shopledger.util.JsonUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardService {

    private final TransactionDao transactionDao = new TransactionDao();
    private final ProductDao productDao = new ProductDao();
    private final ReminderDao reminderDao = new ReminderDao();
    private final StockMovementDao movementDao = new StockMovementDao();
    private final ReminderService reminderService = new ReminderService();

    public Map<String, Object> build() {
        reminderService.reconcileStockReminders();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        BigDecimal todayIncome = transactionDao.sum("INCOME", today, today);
        BigDecimal todayExpense = transactionDao.sum("EXPENSE", today, today);
        BigDecimal yesterdayIncome = transactionDao.sum("INCOME", yesterday, yesterday);
        BigDecimal monthIncome = transactionDao.sum("INCOME", monthStart, monthEnd);
        BigDecimal monthExpense = transactionDao.sum("EXPENSE", monthStart, monthEnd);

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("today", today);
        model.put("todayIncome", todayIncome);
        model.put("todayExpense", todayExpense);
        model.put("todayProfit", todayIncome.subtract(todayExpense));
        model.put("yesterdayIncome", yesterdayIncome);
        model.put("monthIncome", monthIncome);
        model.put("monthExpense", monthExpense);
        model.put("monthProfit", monthIncome.subtract(monthExpense));
        model.put("monthName", today.getMonthValue() + " 月");
        model.put("stockValue", productDao.totalStockValue());
        model.put("productCount", productDao.countAll());
        model.put("lowStockCount", productDao.countLowStock());
        model.put("pendingReminderCount", reminderDao.countPending());
        model.put("overdueReminderCount", reminderDao.countOverdue());
        model.put("inboundThisMonth", movementDao.quantitySum("IN", monthStart, monthEnd));
        model.put("outboundThisMonth", movementDao.quantitySum("OUT", monthStart, monthEnd));

        List<Transaction> recent = transactionDao.recent(8);
        model.put("recentTransactions", recent);

        List<Product> lowStock = productDao.findLowStock(6);
        model.put("lowStockProducts", lowStock);

        List<Reminder> reminders = reminderDao.findUpcoming(7, 5);
        model.put("upcomingReminders", reminders);

        LocalDate trendStart = today.minusDays(13);
        List<Object[]> dailyRows = transactionDao.dailyTotals(trendStart, today);
        Map<LocalDate, BigDecimal[]> dailyMap = new HashMap<>();
        for (Object[] row : dailyRows) {
            dailyMap.put((LocalDate) row[0], new BigDecimal[]{(BigDecimal) row[1], (BigDecimal) row[2]});
        }
        List<String> labels = new ArrayList<>();
        List<BigDecimal> incomeSeries = new ArrayList<>();
        List<BigDecimal> expenseSeries = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        for (LocalDate day = trendStart; !day.isAfter(today); day = day.plusDays(1)) {
            labels.add(day.format(formatter));
            BigDecimal[] values = dailyMap.get(day);
            incomeSeries.add(values == null ? BigDecimal.ZERO : values[0]);
            expenseSeries.add(values == null ? BigDecimal.ZERO : values[1]);
        }
        model.put("trendLabelsJson", JsonUtil.toJson(labels));
        model.put("trendIncomeJson", JsonUtil.toJson(incomeSeries));
        model.put("trendExpenseJson", JsonUtil.toJson(expenseSeries));

        List<Map<String, Object>> pieData = new ArrayList<>();
        for (Object[] row : transactionDao.categoryTotals("EXPENSE", monthStart, monthEnd)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", row[0]);
            item.put("value", row[1]);
            pieData.add(item);
        }
        model.put("expensePieJson", JsonUtil.toJson(pieData));

        List<Object[]> topProducts = movementDao.topProductsBySales(monthStart, monthEnd, 5);
        model.put("topProducts", topProducts);

        return model;
    }
}
