package com.shopledger.service;

import com.shopledger.dao.CategoryDao;
import com.shopledger.dao.ProductDao;
import com.shopledger.dao.SettingDao;
import com.shopledger.dao.StockMovementDao;
import com.shopledger.dao.TransactionDao;
import com.shopledger.dao.UserDao;
import com.shopledger.model.Category;
import com.shopledger.model.Product;
import com.shopledger.model.StockMovement;
import com.shopledger.model.Transaction;
import com.shopledger.model.User;
import com.shopledger.util.Db;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 演示用的“每日经营流水”生成器。
 *
 * <p>每天第一次访问时，如果当天还没有任何收支记录，就生成一批模拟的零售收入
 * 和日常支出，让总览、最近收支和报表在演示时始终有数据。零售会同步写入
 * OUT 库存流水并扣减库存，保证收支与库存账实一致；真实补货仍由
 * {@link StockService} 写入，因此补货会立刻影响“今日支出”。</p>
 *
 * <p>同一天只生成一次：优先看 settings 中的 demo.lastGeneratedDate，
 * 若日期不一致但当天已有流水（例如首次种子数据），也只标记不重复生成。
 * 旧版本只写收支、没有扣库存的模拟销售，会在启动后自动补写库存流水。</p>
 */
public class DailyActivityService {

    private static final Logger LOG = Logger.getLogger(DailyActivityService.class.getName());
    private static final String LAST_DATE_KEY = "demo.lastGeneratedDate";
    private static final String[] PAYMENTS = {"微信", "支付宝", "现金", "银行卡"};
    private static final Object LOCK = new Object();
    private static LocalDate lastChecked;

    private final UserDao userDao = new UserDao();
    private final ProductDao productDao = new ProductDao();
    private final CategoryDao categoryDao = new CategoryDao();
    private final TransactionDao transactionDao = new TransactionDao();
    private final StockMovementDao movementDao = new StockMovementDao();
    private final SettingDao settingDao = new SettingDao();

    public void ensureToday() {
        ensure(LocalDate.now());
    }

    public void ensure(LocalDate day) {
        if (day == null || day.equals(lastChecked)) {
            return;
        }
        synchronized (LOCK) {
            if (day.equals(lastChecked)) {
                return;
            }
            try {
                String last = settingDao.get(LAST_DATE_KEY);
                if (day.toString().equals(last)) {
                    backfillSimulatedSales(day);
                    lastChecked = day;
                    return;
                }
                if (transactionDao.countByDate(day) > 0) {
                    backfillSimulatedSales(day);
                    settingDao.put(LAST_DATE_KEY, day.toString());
                    lastChecked = day;
                    return;
                }
                generate(day);
                lastChecked = day;
            } catch (RuntimeException e) {
                // 自动生成失败不能影响正常记账，下一次请求会重试
                LOG.log(Level.WARNING, "生成每日模拟流水失败：" + e.getMessage(), e);
            }
        }
    }

    private void generate(LocalDate day) {
        User admin = userDao.findByUsername("admin");
        if (admin == null) {
            return;
        }
        List<Product> products = productDao.findAllActive();
        if (products.isEmpty()) {
            return;
        }
        List<Category> incomes = categoryDao.findByType("INCOME");
        List<Category> expenses = categoryDao.findByType("EXPENSE");
        Category sales = pick(incomes, "销售收入");
        Category otherExpense = pick(expenses, "其他支出");
        Category shipping = pick(expenses, "运费");
        Category utility = pick(expenses, "水电");
        if (sales == null || (otherExpense == null && shipping == null && utility == null)) {
            return;
        }

        List<ExpenseTemplate> templates = new ArrayList<>();
        if (otherExpense != null) {
            templates.add(new ExpenseTemplate(otherExpense, 8, 68, "店面杂支"));
        }
        if (shipping != null) {
            templates.add(new ExpenseTemplate(shipping, 6, 32, "配送运费"));
        }
        if (utility != null) {
            templates.add(new ExpenseTemplate(utility, 12, 58, "水电费"));
        }

        int saleCount = (day.getDayOfWeek() == DayOfWeek.SATURDAY
                || day.getDayOfWeek() == DayOfWeek.SUNDAY ? 14 : 11)
                + ThreadLocalRandom.current().nextInt(5);
        int expenseCount = 2 + ThreadLocalRandom.current().nextInt(2);

        Map<Long, Integer> stock = new HashMap<>();
        for (Product product : products) {
            stock.put(product.getId(), product.getStock());
        }

        Db.inTransaction(conn -> {
            for (int i = 0; i < saleCount; i++) {
                Product product = pickAvailable(products, stock);
                if (product == null) {
                    break;
                }
                int available = stock.get(product.getId());
                int quantity = 1 + ThreadLocalRandom.current().nextInt(Math.min(4, available));
                Transaction txn = new Transaction();
                txn.setType("INCOME");
                txn.setCategoryId(sales.getId());
                txn.setAmount(product.getSalePrice().multiply(BigDecimal.valueOf(quantity))
                        .setScale(2, RoundingMode.HALF_UP));
                txn.setTxnDate(day);
                txn.setPaymentMethod(PAYMENTS[ThreadLocalRandom.current().nextInt(PAYMENTS.length)]);
                txn.setRemark("零售：" + product.getName() + " × " + quantity);
                txn.setCreatedBy(admin.getId());
                long txnId = transactionDao.insert(conn, txn);
                recordOutbound(conn, product, quantity, day, admin.getId(), txnId, "前台零售");
                stock.put(product.getId(), available - quantity);
            }

            int expenseOffset = ThreadLocalRandom.current().nextInt(templates.size());
            for (int i = 0; i < expenseCount; i++) {
                ExpenseTemplate template = templates.get((expenseOffset + i) % templates.size());
                int cents = template.minCents + ThreadLocalRandom.current().nextInt(template.maxCents - template.minCents + 1);
                Transaction txn = new Transaction();
                txn.setType("EXPENSE");
                txn.setCategoryId(template.category.getId());
                txn.setAmount(BigDecimal.valueOf(cents, 2));
                txn.setTxnDate(day);
                txn.setPaymentMethod(PAYMENTS[ThreadLocalRandom.current().nextInt(PAYMENTS.length)]);
                txn.setRemark(template.remark);
                txn.setCreatedBy(admin.getId());
                transactionDao.insert(conn, txn);
            }

            settingDao.put(conn, LAST_DATE_KEY, day.toString());
            return null;
        });
    }

    /**
     * 兼容旧数据：早期版本的模拟销售只写了收支，没有写库存流水。
     * 这里按备注还原商品与数量，补一条 OUT 流水并扣减库存；库存不足时跳过。
     */
    private void backfillSimulatedSales(LocalDate day) {
        List<Transaction> pending = transactionDao.findUnlinkedSimulatedSales(day);
        if (pending.isEmpty()) {
            return;
        }
        List<Product> products = productDao.findAllActive();
        Map<String, Product> byName = new HashMap<>();
        Map<Long, Integer> stock = new HashMap<>();
        for (Product product : products) {
            byName.put(product.getName(), product);
            stock.put(product.getId(), product.getStock());
        }
        Db.inTransaction(conn -> {
            for (Transaction txn : pending) {
                String remark = txn.getRemark();
                if (remark == null || !remark.startsWith("零售：")) {
                    continue;
                }
                int separator = remark.lastIndexOf(" × ");
                if (separator <= 3) {
                    continue;
                }
                String name = remark.substring(3, separator).trim();
                int quantity;
                try {
                    quantity = Integer.parseInt(remark.substring(separator + 3).trim());
                } catch (NumberFormatException e) {
                    continue;
                }
                Product product = byName.get(name);
                if (product == null || quantity <= 0) {
                    continue;
                }
                int available = stock.getOrDefault(product.getId(), 0);
                if (available < quantity) {
                    LOG.warning("补记模拟销售库存流水时库存不足：" + name + " 需要 " + quantity
                            + "，当前 " + available);
                    continue;
                }
                recordOutbound(conn, product, quantity, day, txn.getCreatedBy(),
                        txn.getId(), "前台零售");
                stock.put(product.getId(), available - quantity);
            }
            return null;
        });
    }

    private Product pickAvailable(List<Product> products, Map<Long, Integer> stock) {
        List<Product> available = new ArrayList<>();
        for (Product product : products) {
            if (stock.getOrDefault(product.getId(), 0) > 0) {
                available.add(product);
            }
        }
        if (available.isEmpty()) {
            return null;
        }
        return available.get(ThreadLocalRandom.current().nextInt(available.size()));
    }

    /** 写一条销售出库流水，并扣减库存。调用方负责保证库存足够。 */
    private void recordOutbound(Connection conn, Product product, int quantity, LocalDate day,
                                Long operatorId, long transactionId, String note) throws SQLException {
        StockMovement movement = new StockMovement();
        movement.setProductId(product.getId());
        movement.setType("OUT");
        movement.setQuantity(quantity);
        movement.setUnitPrice(product.getSalePrice().setScale(2, RoundingMode.HALF_UP));
        movement.setNote(note);
        movement.setOccurredAt(day);
        movement.setOperatorId(operatorId);
        movement.setTransactionId(transactionId);
        movement.setProductName(product.getName());
        movement.setProductUnit(product.getUnit());
        movementDao.insert(conn, movement);
        productDao.updateStock(conn, product.getId(), -quantity);
    }

    private Category pick(List<Category> categories, String name) {
        for (Category category : categories) {
            if (name.equals(category.getName())) {
                return category;
            }
        }
        return null;
    }

    private static final class ExpenseTemplate {
        private final Category category;
        private final int minCents;
        private final int maxCents;
        private final String remark;

        private ExpenseTemplate(Category category, int minYuan, int maxYuan, String remark) {
            this.category = category;
            this.minCents = minYuan * 100;
            this.maxCents = maxYuan * 100;
            this.remark = remark;
        }
    }
}
