package com.babsnet.posapp.model;

public class PostpaidProduct {
    private String code;
    private String name;
    private Integer status;
    private Long fee;
    private Long komisi;
    private String type;
    private String category;


    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getFee() {
        return fee;
    }

    public void setFee(Long fee) {
        this.fee = fee;
    }

    public Long getKomisi() {
        return komisi;
    }

    public void setKomisi(Long komisi) {
        this.komisi = komisi;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
