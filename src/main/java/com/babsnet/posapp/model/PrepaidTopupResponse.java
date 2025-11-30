package com.babsnet.posapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PrepaidTopupResponse {

    @JsonProperty("data")
    public Data data;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        @JsonProperty("ref_id")       public String refId;
        @JsonProperty("status")       public Integer status;   // 1 = success
        @JsonProperty("product_code") public String productCode;
        @JsonProperty("customer_id")  public String customerId;
        @JsonProperty("price")        public Long price;
        @JsonProperty("message")      public String message;   // "SUCCESS"
        @JsonProperty("balance")      public Long balance;
        @JsonProperty("tr_id")        public Long trId;
        @JsonProperty("rc")           public String rc;        // "00"
        @JsonProperty("sn")           public String sn;        // "123456789"
    }

    /** helper */
    public boolean isSuccess() {
        return data != null && data.status != null && data.status == 1;
    }
}
