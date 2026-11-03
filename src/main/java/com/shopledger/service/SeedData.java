package com.shopledger.service;

import com.shopledger.dao.CategoryDao;
import com.shopledger.dao.ProductDao;
import com.shopledger.dao.ReminderDao;
import com.shopledger.dao.StockMovementDao;
import com.shopledger.dao.TransactionDao;
import com.shopledger.dao.UserDao;
import com.shopledger.model.Category;
import com.shopledger.model.Product;
import com.shopledger.model.Reminder;
import com.shopledger.model.StockMovement;
import com.shopledger.model.Transaction;
import com.shopledger.model.User;
import com.shopledger.util.Db;
import com.shopledger.util.PasswordUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 首次启动时写入演示数据，便于直接体验收支、库存、报表和提醒。
 */
public class SeedData {

    private static final Random RANDOM = new Random(20241020L);
    private static final String[] PAYMENTS = {"微信", "支付宝", "现金", "银行卡"};

    private final UserDao userDao = new UserDao();
    private final CategoryDao categoryDao = new CategoryDao();
    private final ProductDao productDao = new ProductDao();
    private final TransactionDao transactionDao = new TransactionDao();
    private final StockMovementDao movementDao = new StockMovementDao();
    private final ReminderDao reminderDao = new ReminderDao();

    public void seedIfEmpty() {
        if (userDao.count() > 0) {
            return;
        }
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(PasswordUtil.hash("admin123"));
        admin.setDisplayName("店主");
        admin.setRole("ADMIN");
        long adminId = userDao.insert(admin);

        seedCategories();
        List<Product> products = seedProducts();
        seedBusinessData(adminId, products);
        seedReminders();
    }

    private void seedCategories() {
        String[][] income = {
                {"销售收入", "10"}, {"服务收入", "20"}, {"其他收入", "30"}
        };
        String[][] expense = {
                {"进货", "10"}, {"房租", "20"}, {"水电", "30"}, {"工资", "40"}, {"运费", "50"}, {"其他支出", "60"}
        };
        for (String[] row : income) {
            createCategory(row[0], "INCOME", Integer.parseInt(row[1]));
        }
        for (String[] row : expense) {
            createCategory(row[0], "EXPENSE", Integer.parseInt(row[1]));
        }
    }

    private void createCategory(String name, String type, int sortOrder) {
        Category category = new Category();
        category.setName(name);
        category.setType(type);
        category.setSortOrder(sortOrder);
        categoryDao.insert(category);
    }

    private List<Product> seedProducts() {
        Object[][] data = {
                {"SP001", "可口可乐 500ml", "饮料", "瓶", "2.30", "3.50", 46, 12},
                {"SP002", "农夫山泉 550ml", "饮料", "瓶", "1.10", "2.00", 8, 12},
                {"SP003", "乐事薯片 原味", "零食", "袋", "4.20", "6.50", 32, 8},
                {"SP004", "德芙巧克力", "零食", "块", "5.60", "8.00", 5, 8},
                {"SP005", "康师傅红烧牛肉面", "方便食品", "桶", "3.40", "5.00", 58, 15},
                {"SP006", "伊利纯牛奶 250ml", "饮料", "盒", "2.60", "4.00", 27, 10},
                {"SP007", "桃李面包 切片", "烘焙", "袋", "4.80", "7.00", 3, 6},
                {"SP008", "雀巢速溶咖啡", "饮料", "条", "1.50", "2.50", 64, 20},
                {"SP009", "蓝月亮洗衣液 1kg", "日用品", "瓶", "18.00", "25.90", 12, 5},
                {"SP010", "维达抽纸 3层", "日用品", "包", "2.90", "4.50", 40, 10},
                {"SP011", "南孚 5号电池 2粒", "日用品", "板", "5.00", "8.00", 18, 6},
                {"SP012", "天堂折叠雨伞", "应急商品", "把", "22.00", "35.00", 4, 5}
        };
        List<Product> products = new ArrayList<>();
        for (Object[] row : data) {
            Product product = new Product();
            product.setSku((String) row[0]);
            product.setName((String) row[1]);
            product.setCategory((String) row[2]);
            product.setUnit((String) row[3]);
            product.setCostPrice(new BigDecimal((String) row[4]));
            product.setSalePrice(new BigDecimal((String) row[5]));
            product.setStock((Integer) row[6]);
            product.setAlertThreshold((Integer) row[7]);
            product.setStatus(1);
            long id = productDao.insert(product);
            product.setId(id);
            products.add(product);
        }
        return products;
    }

    private void seedBusinessData(long adminId, List<Product> products) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(59);

        // 先随机生成销售明细，再按“期初入库 + 销售出库 = 当前库存”补入库记录
        List<Sale> sales = new ArrayList<>();
        int[] soldTotals = new int[products.size()];
        for (LocalDate day = start; !day.isAfter(today); day = day.plusDays(1)) {
            int base = day.getDayOfWeek().getValue() >= 6 ? 13 : 10;
            int count = base + RANDOM.nextInt(4);
            for (int i = 0; i < count; i++) {
                int index = RANDOM.nextInt(products.size());
                Product product = products.get(index);
                int quantity = 1 + RANDOM.nextInt(4);
                Sale sale = new Sale();
                sale.day = day;
                sale.product = product;
                sale.quantity = quantity;
                sale.unitPrice = product.getSalePrice();
                sales.add(sale);
                soldTotals[index] += quantity;
            }
        }

        Category salesCategory = categoryByName("销售收入", "INCOME");
        Category serviceCategory = categoryByName("服务收入", "INCOME");
        Category otherIncomeCategory = categoryByName("其他收入", "INCOME");
        Category purchaseCategory = categoryByName("进货", "EXPENSE");
        Category rentCategory = categoryByName("房租", "EXPENSE");
        Category utilityCategory = categoryByName("水电", "EXPENSE");
        Category salaryCategory = categoryByName("工资", "EXPENSE");
        Category shippingCategory = categoryByName("运费", "EXPENSE");
        Category otherExpenseCategory = categoryByName("其他支出", "EXPENSE");

        Db.inTransaction(conn -> {
            for (int i = 0; i < products.size(); i++) {
                Product product = products.get(i);
                StockMovement initial = new StockMovement();
                initial.setProductId(product.getId());
                initial.setType("IN");
                initial.setQuantity(product.getStock() + soldTotals[i]);
                initial.setUnitPrice(product.getCostPrice());
                initial.setNote("期初入库");
                initial.setOccurredAt(today.minusDays(75));
                initial.setOperatorId(adminId);
                movementDao.insert(conn, initial);
            }

            for (Sale sale : sales) {
                BigDecimal amount = sale.unitPrice.multiply(BigDecimal.valueOf(sale.quantity))
                        .setScale(2, RoundingMode.HALF_UP);
                Transaction txn = new Transaction();
                txn.setType("INCOME");
                txn.setCategoryId(salesCategory.getId());
                txn.setAmount(amount);
                txn.setTxnDate(sale.day);
                txn.setPaymentMethod(PAYMENTS[RANDOM.nextInt(PAYMENTS.length)]);
                txn.setRemark("零售：" + sale.product.getName() + " × " + sale.quantity);
                txn.setCreatedBy(adminId);
                long txnId = transactionDao.insert(conn, txn);

                StockMovement movement = new StockMovement();
                movement.setProductId(sale.product.getId());
                movement.setType("OUT");
                movement.setQuantity(sale.quantity);
                movement.setUnitPrice(sale.unitPrice);
                movement.setNote("前台零售");
                movement.setOccurredAt(sale.day);
                movement.setOperatorId(adminId);
                movement.setTransactionId(txnId);
                movementDao.insert(conn, movement);
            }

            for (LocalDate day = start; !day.isAfter(today); day = day.plusDays(1)) {
                int dayOfMonth = day.getDayOfMonth();
                if (dayOfMonth == 1) {
                    insertExpense(conn, adminId, rentCategory, new BigDecimal("1400.00"), day, "门店房租");
                    insertExpense(conn, adminId, salaryCategory, new BigDecimal("900.00"), day, "兼职员工工资");
                }
                if (dayOfMonth == 5) {
                    insertExpense(conn, adminId, utilityCategory,
                            BigDecimal.valueOf(120 + RANDOM.nextInt(140)), day, "水电费");
                }
                if (day.getDayOfWeek().getValue() == 1) {
                    insertExpense(conn, adminId, purchaseCategory,
                            BigDecimal.valueOf(260 + RANDOM.nextInt(340)), day, "周补货");
                    insertExpense(conn, adminId, shippingCategory,
                            BigDecimal.valueOf(10 + RANDOM.nextInt(18)), day, "进货运费");
                }
                if (RANDOM.nextInt(100) < 12) {
                    Category category = RANDOM.nextBoolean() ? otherExpenseCategory : serviceCategory;
                    if (category == serviceCategory) {
                        insertIncome(conn, adminId, serviceCategory,
                                BigDecimal.valueOf(30 + RANDOM.nextInt(120)), day, "代收快递服务费");
                    } else {
                        insertExpense(conn, adminId, otherExpenseCategory,
                                BigDecimal.valueOf(10 + RANDOM.nextInt(60)), day, "零星支出");
                    }
                }
                if (RANDOM.nextInt(100) < 4) {
                    insertIncome(conn, adminId, otherIncomeCategory,
                            BigDecimal.valueOf(20 + RANDOM.nextInt(80)), day, "废品回收");
                }
            }
            return null;
        });
    }

    private void insertExpense(Connection conn, long adminId, Category category, BigDecimal amount,
                               LocalDate day, String remark) throws java.sql.SQLException {
        Transaction txn = new Transaction();
        txn.setType("EXPENSE");
        txn.setCategoryId(category.getId());
        txn.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        txn.setTxnDate(day);
        txn.setPaymentMethod(PAYMENTS[RANDOM.nextInt(PAYMENTS.length)]);
        txn.setRemark(remark);
        txn.setCreatedBy(adminId);
        transactionDao.insert(conn, txn);
    }

    private void insertIncome(Connection conn, long adminId, Category category, BigDecimal amount,
                              LocalDate day, String remark) throws java.sql.SQLException {
        Transaction txn = new Transaction();
        txn.setType("INCOME");
        txn.setCategoryId(category.getId());
        txn.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        txn.setTxnDate(day);
        txn.setPaymentMethod(PAYMENTS[RANDOM.nextInt(PAYMENTS.length)]);
        txn.setRemark(remark);
        txn.setCreatedBy(adminId);
        transactionDao.insert(conn, txn);
    }

    private void seedReminders() {
        Object[][] data = {
                {"缴纳本月房租", "每月 1 日通过银行转账给房东，记得保留回单。", -2, "HIGH"},
                {"补货：农夫山泉/面包", "库存已低于预警线，联系供应商安排补货。", 0, "HIGH"},
                {"核对上周营业款", "核对微信、支付宝与现金收款，确认无遗漏。", 2, "MEDIUM"},
                {"营业执照年检", "准备营业执照副本与经营场所证明。", 6, "MEDIUM"},
                {"会员日促销准备", "整理促销海报与小礼品，提前在门口张贴。", 12, "LOW"}
        };
        for (Object[] row : data) {
            Reminder reminder = new Reminder();
            reminder.setTitle((String) row[0]);
            reminder.setContent((String) row[1]);
            reminder.setDueDate(LocalDate.now().plusDays((Integer) row[2]));
            reminder.setPriority((String) row[3]);
            reminder.setDone(false);
            reminderDao.insert(reminder);
        }
    }

    private Category categoryByName(String name, String type) {
        for (Category category : categoryDao.findByType(type)) {
            if (name.equals(category.getName())) {
                return category;
            }
        }
        throw new IllegalStateException("缺少演示分类：" + name);
    }

    private static class Sale {
        private LocalDate day;
        private Product product;
        private int quantity;
        private BigDecimal unitPrice;
    }
}
