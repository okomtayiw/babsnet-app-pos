package com.babsnet.posapp.model;

public class UnitProduct {
    private int id;
    private String name;
    private String abbreviation;

    public UnitProduct() {}

    public UnitProduct(int id, String name, String abbreviation) {
        this.id = id;
        this.name = name;
        this.abbreviation = abbreviation;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAbbreviation() { return abbreviation; }
    public void setAbbreviation(String abbreviation) { this.abbreviation = abbreviation; }
}
