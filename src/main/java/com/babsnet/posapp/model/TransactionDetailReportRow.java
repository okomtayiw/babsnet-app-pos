package com.babsnet.posapp.model;

public class TransactionDetailReportRow {
    private String transDate;
    private String transactionNumber;
    private String productName;
    private int qty;
    private double buyPrice;
    private double price;
    private double subtotal;
    private String paymentMethod;
    private String userName;

    public TransactionDetailReportRow(String transDate, String transactionNumber, String productName, int qty, double buyPrice, double price, double subtotal, String paymentMethod, String userName) {
        this.transDate = transDate;
        this.transactionNumber = transactionNumber;
        this.productName = productName;
        this.qty = qty;
        this.buyPrice = buyPrice;
        this.price = price;
        this.subtotal = subtotal;
        this.paymentMethod = paymentMethod;
        this.userName = userName;
    }

    public String getTransDate() {
        return transDate;
    }

    public void setTransDate(String transDate) {
        this.transDate = transDate;
    }

    public String getTransactionNumber() {
        return transactionNumber;
    }

    public void setTransactionNumber(String transactionNumber) {
        this.transactionNumber = transactionNumber;
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

    public double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
