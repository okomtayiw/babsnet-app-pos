package com.babsnet.posapp.model;


public class PrepaidTopupRequest {
    public String customer_id;
    public String product_code;
    public String ref_id;
    public String username;
    public String sign;

    public PrepaidTopupRequest(String customer_id, String product_code, String ref_id, String username, String sign) {
        this.customer_id = customer_id;
        this.product_code = product_code;
        this.ref_id = ref_id;
        this.username = username;
        this.sign = sign;
    }
}

