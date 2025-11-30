package com.babsnet.posapp.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OnlineTxnJoined {
    private long id;
    private String transactionNumber;
    private LocalDateTime transDate;
    private String status;
    private String customerId;
    private String category;

    // detail
    private String productCode;
    private String description;
    private int qty;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    // provider
    private String rc;
    private String providerMessage;
    private String refId;

    // getters/setters (wajib untuk PropertyValueFactory)
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTransactionNumber() { return transactionNumber; }
    public void setTransactionNumber(String transactionNumber) { this.transactionNumber = transactionNumber; }
    public LocalDateTime getTransDate() { return transDate; }
    public void setTransDate(LocalDateTime transDate) { this.transDate = transDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public String getRc() { return rc; }
    public void setRc(String rc) { this.rc = rc; }
    public String getProviderMessage() { return providerMessage; }
    public void setProviderMessage(String providerMessage) { this.providerMessage = providerMessage; }
    public String getRefId() { return refId; }
    public void setRefId(String refId) { this.refId = refId; }
}

