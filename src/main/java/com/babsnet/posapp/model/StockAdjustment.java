package com.babsnet.posapp.model;

public class StockAdjustment {
    private int id;
    private int productId;
    private String barcode;
    private String productName;
    private String reason;
    private int quantity;
    private String description;
    private String adjustDate;

    public StockAdjustment(int id, int productId, String barcode, String productName, String reason, int quantity, String description, String adjustDate) {
        this.id = id;
        this.productId = productId;
        this.barcode = barcode;
        this.productName = productName;
        this.reason = reason;
        this.quantity = quantity;
        this.description = description;
        this.adjustDate = adjustDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAdjustDate() {
        return adjustDate;
    }

    public void setAdjustDate(String adjustDate) {
        this.adjustDate = adjustDate;
    }
}
