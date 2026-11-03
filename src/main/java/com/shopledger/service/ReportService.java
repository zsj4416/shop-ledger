package com.shopledger.service;

import com.shopledger.dao.StockMovementDao;
import com.shopledger.dao.TransactionDao;
import com.shopledger.util.JsonUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportService {

    private final TransactionDao transactionDao = new TransactionDao();
    private final StockMovementDao movementDao = new StockMovementDao();

    public Map<String, Object> build(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("请选择报表日期范围");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
        if (start.plusDays(732).isBefore(end)) {
            throw new IllegalArgumentException("报表跨度最多两年");
        }

        BigDecimal income = transactionDao.sum("INCOME", start, end);
        BigDecimal expense = transactionDao.sum("EXPENSE", start, end);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("start", start);
        result.put("end", end);
        result.put("income", income);
        result.put("expense", expense);
        result.put("profit", income.subtract(expense));
        long transactionCount = transactionDao.count(null, null, start, end, null);
        result.put("transactionCount", transactionCount);
        BigDecimal avgAmount = transactionCount > 0
                ? income.add(expense).divide(BigDecimal.valueOf(transactionCount), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        result.put("avgAmount", avgAmount);

        List<Object[]> dailyRows = transactionDao.dailyTotals(start, end);
        Map<LocalDate, BigDecimal[]> dailyMap = new HashMap<>();
        for (Object[] row : dailyRows) {
            dailyMap.put((LocalDate) row[0], new BigDecimal[]{(BigDecimal) row[1], (BigDecimal) row[2]});
        }
        List<String> labels = new ArrayList<>();
        List<BigDecimal> incomeSeries = new ArrayList<>();
        List<BigDecimal> expenseSeries = new ArrayList<>();
        DateTimeFormatter labelFormat = DateTimeFormatter.ofPattern("MM-dd");
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            labels.add(day.format(labelFormat));
            BigDecimal[] values = dailyMap.get(day);
            incomeSeries.add(values == null ? BigDecimal.ZERO : values[0]);
            expenseSeries.add(values == null ? BigDecimal.ZERO : values[1]);
        }
        result.put("dailyLabelsJson", JsonUtil.toJson(labels));
        result.put("incomeSeriesJson", JsonUtil.toJson(incomeSeries));
        result.put("expenseSeriesJson", JsonUtil.toJson(expenseSeries));

        List<Object[]> expenseCategories = transactionDao.categoryTotals("EXPENSE", start, end);
        List<Object[]> incomeCategories = transactionDao.categoryTotals("INCOME", start, end);
        result.put("expenseCategories", expenseCategories);
        result.put("incomeCategories", incomeCategories);
        result.put("expenseCategoryJson", JsonUtil.toJson(categoryChart(expenseCategories, expense)));
        result.put("incomeCategoryJson", JsonUtil.toJson(categoryChart(incomeCategories, income)));

        YearMonth endMonth = YearMonth.from(end);
        LocalDate monthStart = endMonth.minusMonths(5).atDay(1);
        Map<String, BigDecimal[]> monthMap = new HashMap<>();
        for (Object[] row : transactionDao.monthlyTotals(monthStart, end)) {
            monthMap.put((String) row[0], new BigDecimal[]{(BigDecimal) row[1], (BigDecimal) row[2]});
        }
        List<String> monthLabels = new ArrayList<>();
        List<BigDecimal> monthIncome = new ArrayList<>();
        List<BigDecimal> monthExpense = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = endMonth.minusMonths(i);
            String key = month.toString();
            monthLabels.add(key);
            BigDecimal[] values = monthMap.get(key);
            monthIncome.add(values == null ? BigDecimal.ZERO : values[0]);
            monthExpense.add(values == null ? BigDecimal.ZERO : values[1]);
        }
        result.put("monthLabelsJson", JsonUtil.toJson(monthLabels));
        result.put("monthIncomeJson", JsonUtil.toJson(monthIncome));
        result.put("monthExpenseJson", JsonUtil.toJson(monthExpense));

        List<Object[]> topProducts = movementDao.topProductsBySales(start, end, 8);
        result.put("topProducts", topProducts);
        result.put("topProductJson", JsonUtil.toJson(topProductChart(topProducts)));

        return result;
    }

    public String exportCsv(LocalDate start, LocalDate end) {
        List<com.shopledger.model.Transaction> items = transactionDao
                .search(null, null, start, end, "", 1, 10000).getItems();
        StringBuilder sb = new StringBuilder();
        sb.append("日期,类型,分类,金额,支付方式,备注,记录人\n");
        for (com.shopledger.model.Transaction txn : items) {
            sb.append(txn.getDateText()).append(',')
                    .append(txn.getTypeText()).append(',')
                    .append(csv(txn.getCategoryName())).append(',')
                    .append(txn.getAmount().toPlainString()).append(',')
                    .append(csv(txn.getPaymentMethod())).append(',')
                    .append(csv(txn.getRemark())).append(',')
                    .append(csv(txn.getCreatedByName())).append('\n');
        }
        return sb.toString();
    }

    private List<Map<String, Object>> categoryChart(List<Object[]> rows, BigDecimal total) {
        List<Map<String, Object>> chart = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", row[0]);
            item.put("value", row[1]);
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                item.put("percent", ((BigDecimal) row[1])
                        .multiply(BigDecimal.valueOf(100))
                        .divide(total, 1, java.math.RoundingMode.HALF_UP));
            } else {
                item.put("percent", BigDecimal.ZERO);
            }
            chart.add(item);
        }
        return chart;
    }

    private List<Map<String, Object>> topProductChart(List<Object[]> rows) {
        List<Map<String, Object>> chart = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", row[0]);
            item.put("quantity", row[1]);
            item.put("amount", row[2]);
            chart.add(item);
        }
        return chart;
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
