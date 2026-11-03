package com.shopledger.model;

import java.math.BigDecimal;

public class Product {

    private Long id;
    private String sku;
    private String name;
    private String category;
    private String unit;
    private BigDecimal costPrice;
    private BigDecimal salePrice;
    private int stock;
    private int alertThreshold;
    private int status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getAlertThreshold() {
        return alertThreshold;
    }

    public void setAlertThreshold(int alertThreshold) {
        this.alertThreshold = alertThreshold;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isLowStock() {
        return status == 1 && stock <= alertThreshold;
    }

    public boolean isOutOfStock() {
        return stock <= 0;
    }

    public boolean isActive() {
        return status == 1;
    }

    public BigDecimal getStockCostValue() {
        BigDecimal price = costPrice == null ? BigDecimal.ZERO : costPrice;
        return price.multiply(BigDecimal.valueOf(stock));
    }

    public BigDecimal getGrossMargin() {
        BigDecimal cost = costPrice == null ? BigDecimal.ZERO : costPrice;
        BigDecimal sale = salePrice == null ? BigDecimal.ZERO : salePrice;
        return sale.subtract(cost);
    }

    public String getStockText() {
        return stock + (unit == null ? "" : unit);
    }
}
