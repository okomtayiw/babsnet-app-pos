package com.babsnet.posapp.model;

public class CheckTopupRequest {
    public String ref_id;
    public String username;
    public String sign;

    public CheckTopupRequest(String ref_id, String username, String sign) {
        this.ref_id = ref_id;
        this.username = username;
        this.sign = sign;
    }

    public String getRef_id() {
        return ref_id;
    }

    public void setRef_id(String ref_id) {
        this.ref_id = ref_id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }
}
