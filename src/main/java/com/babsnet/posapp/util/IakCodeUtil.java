package com.babsnet.posapp.util;

import java.util.Map;
import java.util.Optional;

import static java.util.Map.entry;

/**
 * Util untuk mengklasifikasikan hasil transaksi IAK (HTTP status + rc) menjadi:
 *  - SUCCESS
 *  - FAILED
 *  - PENDING
 *
 * Lengkap dengan deskripsi & solusi yang disarankan.
 */
public class IakCodeUtil {

    private IakCodeUtil() {}

    public enum TxnStatus { SUCCESS, FAILED, PENDING }

    /** Info final yang bisa langsung ditampilkan ke user / dicatat di log. */
    public static final class CodeInfo {
        private final String code;         // "00", "39", "HTTP 400", dst
        private final String description;  // "SUCCESS", "PROCESS", "Bad Request", dst
        private final TxnStatus status;    // SUCCESS/FAILED/PENDING
        private final String solution;     // saran tindakan

        public CodeInfo(String code, String description, TxnStatus status, String solution) {
            this.code = code;
            this.description = description;
            this.status = status;
            this.solution = solution;
        }
        public String getCode() { return code; }
        public String getDescription() { return description; }
        public TxnStatus getStatus() { return status; }
        public String getSolution() { return solution; }

        @Override public String toString() {
            return "CodeInfo{code='" + code + "', desc='" + description + "', status=" + status + ", solution='" + solution + "'}";
        }
    }

    // ====== Tabel RC (200-OK area) ======
    private static CodeInfo info(String c, String d, TxnStatus s, String sol) {
        return new CodeInfo(c, d, s, sol);
    }

    private static final Map<String, CodeInfo> RC_MAP = Map.ofEntries(
            entry("00", info("00", "SUCCESS", TxnStatus.SUCCESS, "Transaction success.")),
            entry("06", info("06", "TRANSACTION NOT FOUND", TxnStatus.FAILED, "Periksa ref_id kamu.")),
            entry("07", info("07", "FAILED", TxnStatus.FAILED, "Transaksi gagal. Silakan coba lagi.")),
            entry("10", info("10", "REACH TOP UP LIMIT USING SAME DESTINATION NUMBER IN 1 DAY", TxnStatus.FAILED,
                    "Nomor tujuan mencapai batas harian. Coba lagi besok.")),
            entry("12", info("12", "BALANCE MAXIMUM LIMIT EXCEEDED", TxnStatus.FAILED, "Batasi saldo sesuai limit.")),
            entry("13", info("13", "CUSTOMER NUMBER BLOCKED", TxnStatus.FAILED, "Gunakan nomor lain atau hubungi CS.")),
            entry("14", info("14", "INCORRECT DESTINATION NUMBER", TxnStatus.FAILED, "Periksa kembali nomor (customer_id).")),
            entry("16", info("16", "NUMBER NOT MATCH WITH OPERATOR", TxnStatus.FAILED, "Nomor tidak cocok dengan operator. Periksa nomor/produk.")),
            entry("17", info("17", "INSUFFICIENT DEPOSIT", TxnStatus.FAILED, "Saldo kurang. Top up deposit dulu.")),
            entry("18", info("18", "NUMBER NOT AVAILABLE", TxnStatus.FAILED, "Lihat daftar E-SIM number yang tersedia (E-SIM List API).")),
            entry("19", info("19", "NUMBER IS ALREADY IN USE", TxnStatus.FAILED, "Pilih E-SIM number lain.")),
            entry("20", info("20", "CODE NOT FOUND", TxnStatus.FAILED, "product_code tidak ditemukan. Cek lewat Pricelist API.")),
            entry("21", info("21", "NUMBER EXPIRED", TxnStatus.FAILED, "Nomor kadaluarsa. Gunakan nomor lain.")),
            entry("39", info("39", "PROCESS", TxnStatus.PENDING, "Sedang diproses. Lanjutkan polling check-status / tunggu callback.")),
            entry("102", info("102", "INVALID IP ADDRESS", TxnStatus.FAILED, "Whitelist IP di developer.iak.id (Prod Setting).")),
            entry("106", info("106", "PRODUCT IS TEMPORARILY OUT OF SERVICE", TxnStatus.FAILED, "Pilih product_code lain yang aktif.")),
            entry("107", info("107", "ERROR IN XML FORMAT", TxnStatus.FAILED, "Perbaiki body request (JSON/XML) sesuai dokumen API.")),
            entry("110", info("110", "SYSTEM UNDER MAINTENANCE", TxnStatus.FAILED, "Coba lagi nanti (maintenance).")),
            entry("117", info("117", "PAGE NOT FOUND", TxnStatus.FAILED, "Periksa URL endpoint API.")),
            entry("121", info("121", "MONTHLY TOP UP LIMIT EXCEEDED", TxnStatus.FAILED, "Berlaku untuk OVO. Tunggu reset limit bulanan.")),
            entry("131", info("131", "TOP UP REGION BLOCKED FOR PLAYER", TxnStatus.FAILED, "Diblokir menurut region. Gunakan nomor lain.")),
            entry("132", info("132", "PRODUCT CODE NOT ELIGIBLE DUE TO SUBSCRIBER LOCATION", TxnStatus.FAILED, "Pilih product_code lain yang eligible.")),
            entry("141", info("141", "INVALID USER ID / ZONE ID / SERVER ID / ROLENAME", TxnStatus.FAILED, "Cek data player (lihat Inquiry Game Server).")),
            entry("142", info("142", "INVALID USER ID", TxnStatus.FAILED, "User ID tidak valid. Periksa atau gunakan yang lain.")),
            entry("201", info("201", "UNDEFINED RESPONSE CODE", TxnStatus.PENDING, "Kode belum terdefinisi. Hubungi CS.")),
            entry("202", info("202", "MAXIMUM 1 NUMBER 1 TIME IN 1 DAY", TxnStatus.FAILED,
                    "Izinkan multi transaksi di developer.iak.id (API Setting) atau coba besok.")),
            entry("203", info("203", "NUMBER IS TOO LONG", TxnStatus.FAILED, "Panjang customer_id berlebihan. Periksa input.")),
            entry("204", info("204", "WRONG AUTHENTICATION", TxnStatus.FAILED, "Periksa nilai signature (sign).")),
            entry("205", info("205", "WRONG COMMAND", TxnStatus.FAILED, "Periksa field command.")),
            entry("206", info("206", "THIS DESTINATION NUMBER HAS BEEN BLOCKED", TxnStatus.FAILED,
                    "Kelola blacklist/whitelist di API Security (developer.iak.id).")),
            entry("207", info("207", "MAXIMUM 1 NUMBER WITH ANY CODE 1 TIME IN 1 DAY", TxnStatus.FAILED,
                    "Nonaktifkan high restriction di prod-setting atau coba besok.")),
            entry("301", info("301", "EMAIL SEND LIMIT REACHED", TxnStatus.FAILED, "Limit email untuk TRX tersebut tercapai. Coba besok.")),
            // Game only
            entry("143", info("143", "INQUIRY NOT NEEDED", TxnStatus.FAILED, "Operator adalah voucher; tidak perlu inquiry."))
    );


    /**
     * Klasifikasi hanya dari RC (jika kamu tidak pegang HTTP status).
     */
    public static CodeInfo classifyRc(String rc) {
        if (rc == null || rc.isBlank()) {
            return new CodeInfo("RC_NULL", "Missing response code", TxnStatus.FAILED,
                    "Response tidak mengandung rc. Periksa integrasi/parsing.");
        }
        CodeInfo mapped = RC_MAP.get(rc.trim());
        return mapped != null
                ? mapped
                : new CodeInfo(rc.trim(), "UNMAPPED/UNKNOWN CODE", TxnStatus.PENDING,
                "Kode belum dipetakan. Hubungi CS jika berulang.");
    }

    /**
     * Deteksi apakah transaksi masih diproses berdasarkan kombinasi sinyal yang umum di API:
     * - rc = 39 (PROCESS)
     * - message = PROCESS / PROCESSING
     * - status int = 2
     */
    public static boolean isStillProcessing(String rc, String message, Integer statusInt) {
        if ("39".equals(rc)) return true;
        String msg = message == null ? "" : message.trim();
        if ("PROCESS".equalsIgnoreCase(msg) || "PROCESSING".equalsIgnoreCase(msg)) return true;
        return statusInt != null && statusInt == 2;
    }



    public static ApiClient buildApiClient() {
        final String profile = ConfigUtil.get("profile");

        final boolean isProd = "prod".equals(profile);
        String userKey = ConfigUtil.get("username");
        String tokenKey;
        String urlPrepaid;
        String urlPostpaid;
        if(isProd) {
            tokenKey = "token.prod";
            urlPrepaid = "url.prepaid.prod";
            urlPostpaid = "url.postpaid.prod";
        } else {
            tokenKey = "token.dev";
            urlPrepaid = "url.prepaid.dev";
            urlPostpaid = "url.postpaid.dev";
        }


        final String username = Optional.ofNullable(userKey)
                .filter(s -> !s.isBlank())
                .orElse(null);

        final String token = Optional.ofNullable(ConfigUtil.get(tokenKey))
                .filter(s -> !s.isBlank())
                .orElse(null);

        final String urlPre = Optional.ofNullable(ConfigUtil.get(urlPrepaid))
                .filter(s -> !s.isBlank())
                .orElse(null);

        final String urlPost = Optional.ofNullable(ConfigUtil.get(urlPostpaid))
                .filter(s -> !s.isBlank())
                .orElse(null);

        return new ApiClient(urlPre, urlPost,username, token);
    }

}
