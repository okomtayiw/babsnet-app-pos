package com.babsnet.posapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InquiryResponsePostPaid {
    public Data data;
    public List<Object> meta;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        public Long tr_id;
        public String code;
        public String datetime;
        public String hp;
        public String tr_name;
        public String period;
        public BigDecimal nominal;
        public BigDecimal admin;
        public String ref_id;
        public String response_code;
        public String message;
        public BigDecimal price;
        public BigDecimal selling_price;

        public String noref;
        public Long balance;

        public JsonNode desc;
    }
}
