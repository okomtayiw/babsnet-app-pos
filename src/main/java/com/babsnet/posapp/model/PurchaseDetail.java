package com.babsnet.posapp.model;



public class PurchaseDetail {

    private int id;
    private int purchaseId;
    private Product product;
    private int quantity;
    private double buyPrice;
    private double subtotal;

    public PurchaseDetail() {}

    public PurchaseDetail(int id, int purchaseId, Product product, int quantity, double buyPrice, double subtotal) {
        this.id = id;
        this.purchaseId = purchaseId;
        this.product = product;
        this.quantity = quantity;
        this.buyPrice = buyPrice;
        this.subtotal = subtotal;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(int purchaseId) {
        this.purchaseId = purchaseId;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        updateSubtotal();
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
        updateSubtotal();
    }

    public double getSubtotal() {
        return subtotal;
    }

    private void updateSubtotal() {
        this.subtotal = this.quantity * this.buyPrice;
    }
}

