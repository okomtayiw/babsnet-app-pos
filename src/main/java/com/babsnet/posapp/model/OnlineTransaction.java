package com.babsnet.posapp.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OnlineTransaction {
    public Long id;
    public String transactionNumber;
    public LocalDateTime transDate;
    public String paymentMethod;
    public OnlineTxnStatus status = OnlineTxnStatus.PENDING;
    public Long userId;
    public BigDecimal total = BigDecimal.ZERO;

    // provider fields
    public String provider = "IAK";
    public String category;              // pulsa/data/pln/...
    public String productCode;
    public String customerId;
    public String refId;                 // UNIQUE (idempotency)
    public Long providerTrId;
    public String sn;
    public String rc;
    public String providerMessage;
    public Integer providerStatus;       // 1 = success, 2 = not success

    public Long denom;                   // optional
    public BigDecimal buyPrice;
    public BigDecimal sellPrice;
    public BigDecimal fee;
    public BigDecimal margin;

    public String inquiryJson;           // simpan JSON (optional)
    public String lastResponseJson;

    public List<OnlineTransactionDetail> details = new ArrayList<>();
}
