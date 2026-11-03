package com.shopledger.service;

import com.shopledger.dao.CategoryDao;
import com.shopledger.model.Category;

import java.util.List;

public class CategoryService {

    private final CategoryDao categoryDao = new CategoryDao();

    public List<Category> findAll() {
        return categoryDao.findAll();
    }

    public List<Category> listWithUsage() {
        List<Category> list = categoryDao.findAll();
        for (Category category : list) {
            category.setTransactionCount(categoryDao.countTransactions(category.getId()));
        }
        return list;
    }

    public List<Category> findByType(String type) {
        return categoryDao.findByType(type);
    }

    public long create(String name, String type, int sortOrder) {
        validate(name, type);
        Category category = new Category();
        category.setName(name.trim());
        category.setType(type);
        category.setSortOrder(sortOrder);
        return categoryDao.insert(category);
    }

    public void update(long id, String name, String type, int sortOrder) {
        validate(name, type);
        Category existing = categoryDao.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("分类不存在");
        }
        if (categoryDao.countTransactions(id) > 0 && !existing.getType().equals(type)) {
            throw new IllegalArgumentException("该分类已被收支记录使用，不能修改收支类型");
        }
        Category category = new Category();
        category.setId(id);
        category.setName(name.trim());
        category.setType(type);
        category.setSortOrder(sortOrder);
        categoryDao.update(category);
    }

    public void delete(long id) {
        if (categoryDao.countTransactions(id) > 0) {
            throw new IllegalArgumentException("该分类已被收支记录使用，无法删除");
        }
        categoryDao.delete(id);
    }

    private void validate(String name, String type) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("请输入分类名称");
        }
        if (name.trim().length() > 50) {
            throw new IllegalArgumentException("分类名称不能超过 50 个字符");
        }
        if (!"INCOME".equals(type) && !"EXPENSE".equals(type)) {
            throw new IllegalArgumentException("分类类型不合法");
        }
    }
}
