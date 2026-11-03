package com.shopledger.service;

import com.shopledger.dao.CategoryDao;
import com.shopledger.dao.TransactionDao;
import com.shopledger.model.Category;
import com.shopledger.model.Transaction;
import com.shopledger.util.Page;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionService {

    private final TransactionDao transactionDao = new TransactionDao();
    private final CategoryDao categoryDao = new CategoryDao();

    public Page<Transaction> search(String type, Long categoryId, LocalDate start, LocalDate end,
                                    String keyword, int page, int size) {
        return transactionDao.search(type, categoryId, start, end, keyword, Math.max(1, page), size);
    }

    public Transaction findById(long id) {
        return transactionDao.findById(id);
    }

    public long save(Transaction txn) {
        validate(txn);
        return transactionDao.insert(txn);
    }

    public void update(Transaction txn) {
        validate(txn);
        transactionDao.update(txn);
    }

    public void delete(long id) {
        transactionDao.delete(id);
    }

    public BigDecimal sum(String type, LocalDate start, LocalDate end) {
        return transactionDao.sum(type, start, end);
    }

    private void validate(Transaction txn) {
        if (txn.getAmount() == null || txn.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("金额必须大于 0");
        }
        if (txn.getAmount().compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new IllegalArgumentException("金额过大，请检查");
        }
        if (txn.getTxnDate() == null) {
            throw new IllegalArgumentException("请选择记账日期");
        }
        if (txn.getTxnDate().isAfter(LocalDate.now().plusDays(1))) {
            throw new IllegalArgumentException("记账日期不能晚于明天");
        }
        if (txn.getPaymentMethod() == null || txn.getPaymentMethod().isEmpty()) {
            txn.setPaymentMethod("现金");
        }
        Category category = categoryDao.findById(txn.getCategoryId() == null ? 0 : txn.getCategoryId());
        if (category == null) {
            throw new IllegalArgumentException("请选择有效的收支分类");
        }
        if (!category.getType().equals(txn.getType())) {
            throw new IllegalArgumentException("分类与收支类型不匹配");
        }
        if (txn.getRemark() != null && txn.getRemark().length() > 200) {
            throw new IllegalArgumentException("备注不能超过 200 个字符");
        }
    }
}
