package com.babsnet.posapp.model;

import java.time.LocalDate;

public class Purchase {

    private int id;
    private String purchaseNumber;
    private LocalDate purchaseDate;
    private String supplierName;
    private double total;
    private String status;

    public Purchase(int id, String purchaseNumber, LocalDate purchaseDate, String supplierName, double total, String status) {
        this.id = id;
        this.purchaseNumber = purchaseNumber;
        this.purchaseDate = purchaseDate;
        this.supplierName = supplierName;
        this.total = total;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public String getPurchaseNumber() {
        return purchaseNumber;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public double getTotal() {
        return total;
    }

    public String getStatus() {
        return status;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setPurchaseNumber(String purchaseNumber) {
        this.purchaseNumber = purchaseNumber;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
