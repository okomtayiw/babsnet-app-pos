package com.babsnet.posapp.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;

public class Product {
    private int id;
    private String name;
    private int stock;
    private double price;
    private double lastBuyPrice;
    private double discount;
    private String typeName;
    private String barcode;
    private String unitName;

    public Product(int id, String name, int stock, double price,double lastBuyPrice, double discount, String typeName, String barcode) {
        this.id = id;
        this.name = name;
        this.stock = stock;
        this.price = price;
        this.lastBuyPrice = lastBuyPrice;
        this.discount = discount;
        this.typeName = typeName;
        this.barcode = barcode;
    }

    public Product() {

    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getStock() { return stock; }
    public double getPrice() { return price; }
    public double getDiscount() { return discount; }
    public String getTypeName() { return typeName; }

    public String getBarcode() {
        return barcode;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getLastBuyPrice() {
        return lastBuyPrice;
    }

    public void setLastBuyPrice(double lastBuyPrice) {
        this.lastBuyPrice = lastBuyPrice;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean value) {
        selected.set(value);
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    
}

