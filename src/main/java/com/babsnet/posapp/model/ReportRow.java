package com.babsnet.posapp.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReportRow {
    Long id;
    LocalDateTime transDate;
    String status;
    String customerId;
    String refId;
    String rc;
    String providerMessage;
    String category;
    String productCode;
    String description;
    BigDecimal denom;
    BigDecimal buyPrice;
    BigDecimal unitPrice;

    public ReportRow(Long id, LocalDateTime transDate, String status, String customerId, String refId, String rc, String providerMessage, String category, String productCode, String description, BigDecimal denom, BigDecimal buyPrice, BigDecimal unitPrice) {
        this.id = id;
        this.transDate = transDate;
        this.status = status;
        this.customerId = customerId;
        this.refId = refId;
        this.rc = rc;
        this.providerMessage = providerMessage;
        this.category = category;
        this.productCode = productCode;
        this.description = description;
        this.denom = denom;
        this.buyPrice = buyPrice;
        this.unitPrice = unitPrice;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getTransDate() {
        return transDate;
    }

    public void setTransDate(LocalDateTime transDate) {
        this.transDate = transDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    public String getRc() {
        return rc;
    }

    public void setRc(String rc) {
        this.rc = rc;
    }

    public String getProviderMessage() {
        return providerMessage;
    }

    public void setProviderMessage(String providerMessage) {
        this.providerMessage = providerMessage;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getDenom() {
        return denom;
    }

    public void setDenom(BigDecimal denom) {
        this.denom = denom;
    }

    public BigDecimal getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(BigDecimal buyPrice) {
        this.buyPrice = buyPrice;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}

