package com.babsnet.posapp.model;

import java.util.List;

public class Transaction {
    private int id;
    private String transactionNumber;
    private String transDate;
    private String paymentMethod;
    private double total;
    private String status;
    private String userName;
    private List<TransactionDetail> details;

    public Transaction(int id, String transactionNumber, String transDate, String paymentMethod, double total, String status) {
        this.id = id;
        this.transactionNumber = transactionNumber;
        this.transDate = transDate;
        this.paymentMethod = paymentMethod;
        this.total = total;
        this.status = status;
    }

    public Transaction() {

    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTransactionNumber() {
        return transactionNumber;
    }

    public void setTransactionNumber(String transactionNumber) {
        this.transactionNumber = transactionNumber;
    }

    public String getTransDate() {
        return transDate;
    }

    public void setTransDate(String transDate) {
        this.transDate = transDate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<TransactionDetail> getDetails() {
        return details;
    }

    public void setDetails(List<TransactionDetail> details) {
        this.details = details;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
