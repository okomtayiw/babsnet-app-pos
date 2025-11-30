package com.babsnet.posapp.model;

import java.math.BigDecimal;

public class OnlineTransactionDetail {
    public Long id;
    public Long onlineTransactionId;
    public String productCode;
    public String description;
    public int qty = 1;
    public BigDecimal unitPrice = BigDecimal.ZERO;
    public BigDecimal subtotal = BigDecimal.ZERO;
}
