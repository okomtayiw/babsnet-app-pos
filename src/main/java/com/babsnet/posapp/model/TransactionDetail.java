package com.babsnet.posapp.model;

public class TransactionDetail {
    private int id;
    private int transactionId;
    private int productId;
    private String productName;
    private int qty;
    private double price;
    private double subtotal;

    public TransactionDetail(int id, int transactionId, int productId, String productName, int qty, double price, double subtotal) {
        this.id = id;
        this.transactionId = transactionId;
        this.productId = productId;
        this.productName = productName;
        this.qty = qty;
        this.price = price;
        this.subtotal = subtotal;
    }

    public TransactionDetail() {

    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }
}
