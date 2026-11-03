package com.shopledger.service;

import com.shopledger.dao.CategoryDao;
import com.shopledger.dao.ProductDao;
import com.shopledger.dao.StockMovementDao;
import com.shopledger.dao.TransactionDao;
import com.shopledger.model.Category;
import com.shopledger.model.Product;
import com.shopledger.model.StockMovement;
import com.shopledger.model.Transaction;
import com.shopledger.util.Db;
import com.shopledger.util.Page;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StockService {

    private final StockMovementDao movementDao = new StockMovementDao();
    private final ProductDao productDao = new ProductDao();
    private final TransactionDao transactionDao = new TransactionDao();
    private final CategoryDao categoryDao = new CategoryDao();
    private final ReminderService reminderService = new ReminderService();

    public Page<StockMovement> search(Long productId, String type, LocalDate start, LocalDate end,
                                      int page, int size) {
        return movementDao.search(productId, type, start, end, Math.max(1, page), size);
    }

    public List<StockMovement> recent(int limit) {
        return movementDao.recent(limit);
    }

    /**
     * 记录库存变动。IN 入库、OUT 出库、ADJUST 盘点（quantity 为盘点后的实际库存）。
     * 出库时可选择同步生成一笔“销售收入”；入库时可选择同步生成一笔“进货支出”，
     * 这样补货会立即反映到今日支出和最近收支中。
     */
    public StockMovement record(long productId, String type, int quantity, BigDecimal unitPrice,
                                String note, LocalDate occurredAt, Long operatorId,
                                boolean syncIncome, boolean syncExpense) {
        if (!"IN".equals(type) && !"OUT".equals(type) && !"ADJUST".equals(type)) {
            throw new IllegalArgumentException("无效的库存操作类型");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("请选择业务日期");
        }
        if (occurredAt.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("业务日期不能晚于今天");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("数量不能为负数");
        }
        if (("IN".equals(type) || "OUT".equals(type)) && quantity == 0) {
            throw new IllegalArgumentException("数量必须大于 0");
        }

        Product product = productDao.findById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (product.getStatus() != 1) {
            throw new IllegalArgumentException("商品已停用，请先启用后再操作");
        }

        final int[] deltaHolder = new int[1];
        final StockMovement[] resultHolder = new StockMovement[1];

        Db.inTransaction(conn -> {
            Product locked = productDao.lockById(conn, productId);
            if (locked == null) {
                throw new IllegalArgumentException("商品不存在");
            }
            int delta;
            if ("IN".equals(type)) {
                delta = quantity;
            } else if ("OUT".equals(type)) {
                if (locked.getStock() < quantity) {
                    throw new IllegalArgumentException("库存不足：当前库存 " + locked.getStock()
                            + locked.getUnit() + "，本次出库 " + quantity + locked.getUnit());
                }
                delta = -quantity;
            } else {
                delta = quantity - locked.getStock();
            }
            deltaHolder[0] = delta;

            productDao.updateStock(conn, productId, delta);

            BigDecimal price = unitPrice;
            if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
                price = "IN".equals(type) ? locked.getCostPrice() : locked.getSalePrice();
            }
            price = price.setScale(2, RoundingMode.HALF_UP);

            StockMovement movement = new StockMovement();
            movement.setProductId(productId);
            movement.setType(type);
            movement.setQuantity("ADJUST".equals(type) ? delta : quantity);
            movement.setUnitPrice(price);
            movement.setNote(note);
            movement.setOccurredAt(occurredAt);
            movement.setOperatorId(operatorId);
            movement.setProductName(locked.getName());
            movement.setProductUnit(locked.getUnit());

            if (syncExpense && "IN".equals(type)) {
                BigDecimal amount = price.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    Category category = findPurchaseCategory();
                    if (category != null) {
                        String suffix = note == null || note.trim().isEmpty() ? "" : "（" + note.trim() + "）";
                        Transaction txn = new Transaction();
                        txn.setType("EXPENSE");
                        txn.setCategoryId(category.getId());
                        txn.setAmount(amount);
                        txn.setTxnDate(occurredAt);
                        txn.setPaymentMethod("银行转账");
                        txn.setRemark("补货：" + locked.getName() + " × " + quantity
                                + locked.getUnit() + suffix);
                        txn.setCreatedBy(operatorId);
                        long txnId = transactionDao.insert(conn, txn);
                        movement.setTransactionId(txnId);
                    }
                }
            }

            if (syncIncome && "OUT".equals(type)) {
                BigDecimal amount = price.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    Category category = findSalesCategory();
                    if (category != null) {
                        Transaction txn = new Transaction();
                        txn.setType("INCOME");
                        txn.setCategoryId(category.getId());
                        txn.setAmount(amount);
                        txn.setTxnDate(occurredAt);
                        txn.setPaymentMethod("现金");
                        txn.setRemark("销售出库：" + locked.getName() + " × " + quantity + locked.getUnit());
                        txn.setCreatedBy(operatorId);
                        long txnId = transactionDao.insert(conn, txn);
                        movement.setTransactionId(txnId);
                    }
                }
            }

            movementDao.insert(conn, movement);
            resultHolder[0] = movement;
            return null;
        });

        resultHolder[0].setNote((note == null ? "" : note));
        if ("IN".equals(type) || "ADJUST".equals(type)) {
            reminderService.reconcileStockReminders();
        }
        return resultHolder[0];
    }

    public Map<String, Object> summary(LocalDate start, LocalDate end) {
        Map<String, Object> map = new HashMap<>();
        map.put("inQuantity", movementDao.quantitySum("IN", start, end));
        map.put("outQuantity", movementDao.quantitySum("OUT", start, end));
        map.put("inAmount", movementDao.amountSum("IN", start, end));
        map.put("outAmount", movementDao.amountSum("OUT", start, end));
        map.put("stockValue", productDao.totalStockValue());
        map.put("productCount", productDao.countAll());
        map.put("lowStockCount", productDao.countLowStock());
        return map;
    }

    private Category findPurchaseCategory() {
        for (Category category : categoryDao.findByType("EXPENSE")) {
            if ("进货".equals(category.getName())) {
                return category;
            }
        }
        List<Category> expenses = categoryDao.findByType("EXPENSE");
        return expenses.isEmpty() ? null : expenses.get(0);
    }

    private Category findSalesCategory() {
        for (Category category : categoryDao.findByType("INCOME")) {
            if ("销售收入".equals(category.getName())) {
                return category;
            }
        }
        List<Category> incomes = categoryDao.findByType("INCOME");
        return incomes.isEmpty() ? null : incomes.get(0);
    }
}
