package com.babsnet.posapp.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Map ke respons IAK: data.pricelist[] */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceItem {
    @JsonProperty("product_code")
    private String code;

    @JsonProperty("product_description")
    private String description;

    @JsonProperty("product_nominal")
    private String nominal;

    @JsonProperty("product_details")
    private String details;

    @JsonProperty("product_price")
    private Long price;

    @JsonProperty("product_type")
    private String type;

    @JsonProperty("active_period")
    private String activePeriod;

    @JsonProperty("status")
    private String status;

    @JsonProperty("icon_url")
    private String iconUrl;

    @JsonProperty("product_category")
    private String category;

    public String getCode() { return code; }
    public String getDescription() { return description; }
    public String getNominal() { return nominal; }
    public String getDetails() { return details; }
    public Long getPrice() { return price; }
    public String getType() { return type; }
    public String getActivePeriod() { return activePeriod; }
    public String getStatus() { return status; }
    public String getIconUrl() { return iconUrl; }
    public String getCategory() { return category; }

    public void setCode(String code) { this.code = code; }
    public void setDescription(String description) { this.description = description; }
    public void setNominal(String nominal) { this.nominal = nominal; }
    public void setDetails(String details) { this.details = details; }
    public void setPrice(Long price) { this.price = price; }
    public void setType(String type) { this.type = type; }
    public void setActivePeriod(String activePeriod) { this.activePeriod = activePeriod; }
    public void setStatus(String status) { this.status = status; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
    public void setCategory(String category) { this.category = category; }
}

