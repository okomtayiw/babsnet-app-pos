package com.babsnet.posapp.model;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ProductCategoryOnline {
    // Khusus untuk UI filter (semua kategori)
    ALL("*", "Semua Kategori"),

    PULSA("pulsa", "Pulsa"),
    DATA("data", "Data"),
    ETOLL("etoll", "eToll"),
    VOUCHER("voucher", "Voucher"),
    GAME("game", "Game"),
    PLN("pln", "PLN"),
    ESIM_INTERNATIONAL("esiminternational", "eSIM International"),
    INTERNATIONAL("international", "International"),

    // fallback kalau API mengirim kategori yang tidak kita kenal
    UNKNOWN("unknown", "Lainnya");

    private final String apiValue;
    private final String title;

    ProductCategoryOnline(String apiValue, String title) {
        this.apiValue = apiValue;
        this.title = title;
    }

    public String getApiValue() { return apiValue; }
    public String getTitle()    { return title; }

    private static final Map<String, ProductCategoryOnline> BY_API =
            Arrays.stream(values()).collect(Collectors.toMap(
                    pc -> pc.apiValue.toLowerCase(Locale.ROOT),
                    Function.identity()
            ));

    public static ProductCategoryOnline fromApiValue(String s) {
        if (s == null) return UNKNOWN;
        ProductCategoryOnline pc = BY_API.get(s.trim().toLowerCase(Locale.ROOT));
        return pc != null ? pc : UNKNOWN;
    }

    @Override public String toString() { return title; }
}
