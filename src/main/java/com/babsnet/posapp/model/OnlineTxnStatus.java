package com.babsnet.posapp.model;

import com.babsnet.posapp.util.IakCodeUtil;

public enum OnlineTxnStatus {
    PENDING,   // transaksi dibuat / menunggu hasil provider
    SUCCESS,   // berhasil
    FAILED,    // gagal
    CANCELLED; // dibatalkan

    /** Map dari status IAK (SUCCESS/FAILED/PENDING) ke status internal. */
    public static OnlineTxnStatus fromIak(IakCodeUtil.TxnStatus s) {
        if (s == null) return PENDING;
        return switch (s) {
            case SUCCESS -> SUCCESS;
            case FAILED  -> FAILED;
            case PENDING -> PENDING;
        };
    }

    /** Parse aman dari string DB (fallback ke PENDING). */
    public static OnlineTxnStatus fromDb(String s) {
        if (s == null || s.isBlank()) return PENDING;
        try { return OnlineTxnStatus.valueOf(s.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return PENDING; }
    }

    /** Apakah status ini final (tidak perlu polling lagi)? */
    public boolean isFinal() {
        return this == SUCCESS || this == FAILED || this == CANCELLED;
    }
}
