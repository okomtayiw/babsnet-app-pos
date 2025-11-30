package com.babsnet.posapp.util;



import com.babsnet.posapp.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ApiClient {
    // ===== KONFIG =====
    private String urlPrePaid;
    private String urlPostPaid;
    private static final String PATH_PRICELIST = "/api/pricelist";

    // sesuaikan:
    private final String username;         // contoh: "085647642539"
    private final String apiKey;           // API key milikmu
    private final String additional = "pl"; // skema sign dari vendor (ganti jika beda)
    private final String defaultStatus = "all";
    // ===================

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_2)
            .build();

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public ApiClient(String urlPre, String urlPost, String username, String apiKey) {
        urlPrePaid = urlPre;
        urlPostPaid = urlPost;
        this.username = username;
        this.apiKey = apiKey;
    }

    public CompletableFuture<List<PriceItem>> fetchPriceList(String statusOrNull) {
        String status = (statusOrNull == null || statusOrNull.isBlank()) ? defaultStatus : statusOrNull;
        String sign = SignatureUtil.md5Hex(username + apiKey + additional);

        ObjectNode body = mapper.createObjectNode();
        body.put("status", status);
        body.put("username", username);
        body.put("sign", sign);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(urlPrePaid + PATH_PRICELIST))
                .timeout(Duration.ofSeconds(25))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                        throw new RuntimeException("HTTP " + resp.statusCode() + " : " + resp.body());
                    }
                    try {
                        JsonNode root = mapper.readTree(resp.body());
                        JsonNode arr = root.path("data").path("pricelist");
                        if (!arr.isArray()) {
                            throw new RuntimeException("Format tidak sesuai: data.pricelist bukan array");
                        }
                        return mapper.convertValue(arr, new TypeReference<List<PriceItem>>() {});
                    } catch (Exception e) {
                        throw new RuntimeException("Gagal parse JSON: " + e.getMessage(), e);
                    }
                });
    }

    /** Topup pulsa prepaid */
    public PrepaidTopupResponse topupRequest(String customerId, String productCode, String refId) {
        try {
            String sign = SignatureUtil.md5Hex(username + apiKey + refId);

            PrepaidTopupRequest payload = new PrepaidTopupRequest(
                    customerId, productCode, refId, username, sign
            );

            String json = mapper.writeValueAsString(payload);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(urlPrePaid + "/api/top-up"))
                    .timeout(Duration.ofSeconds(25))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new RuntimeException("HTTP " + resp.statusCode() + " : " + resp.body());
            }

            PrepaidTopupResponse out = mapper.readValue(resp.body(), PrepaidTopupResponse.class);
            return out;

        } catch (Exception e) {
            throw new RuntimeException("Topup error: " + e.getMessage(), e);
        }
    }

    public PrepaidTopupResponse checkStatusTopup(String refId) {
        try {
            String sign = SignatureUtil.md5Hex(username + apiKey + refId);

            CheckTopupRequest payload = new CheckTopupRequest(
                    refId, username, sign
            );

            String json = mapper.writeValueAsString(payload);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(urlPrePaid + "/api/check-status"))
                    .timeout(Duration.ofSeconds(25))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new RuntimeException("HTTP " + resp.statusCode() + " : " + resp.body());
            }

            PrepaidTopupResponse out = mapper.readValue(resp.body(), PrepaidTopupResponse.class);
            return out;

        } catch (Exception e) {
            throw new RuntimeException("Topup error: " + e.getMessage(), e);
        }
    }

    public PlnInquiryResponse inquiryPln(String customerId) {
        try {
            String sign = SignatureUtil.md5Hex(username + apiKey + customerId);

            PrepaidTopupRequest payload = new PrepaidTopupRequest(
                    customerId, null, null, username, sign
            );

            String json = mapper.writeValueAsString(payload);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(urlPrePaid + "/api/inquiry-pln"))
                    .timeout(Duration.ofSeconds(25))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new RuntimeException("HTTP " + resp.statusCode() + " : " + resp.body());
            }

            PlnInquiryResponse out = mapper.readValue(resp.body(), PlnInquiryResponse.class);
            return out;

        } catch (Exception e) {
            throw new RuntimeException("Topup error: " + e.getMessage(), e);
        }
    }

    public static String toJsonSafely(Object o) {
        try { return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(o); }
        catch (Exception e) { return null; }
    }

    public CompletableFuture<List<PostpaidProduct>> fetchPostpaidProducts(String statusOrNull) {
        String status = (statusOrNull == null || statusOrNull.isBlank()) ? defaultStatus : statusOrNull;
        String sign = SignatureUtil.md5Hex(username + apiKey + additional);
        ObjectNode body = mapper.createObjectNode();
        body.put("commands", "pricelist-pasca");
        body.put("status", status);
        body.put("username", username);
        body.put("sign", sign);

        String endpointUrl = urlPostPaid + "/api/v1/bill/check"; // misal: /price-list/postpaid
        HttpRequest req = HttpRequest.newBuilder(URI.create(endpointUrl))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(resp -> {
                    if (resp.statusCode() / 100 != 2) {
                        throw new RuntimeException("HTTP " + resp.statusCode() + ": " + resp.body());
                    }
                    try {
                        JsonNode root = mapper.readTree(resp.body());
                        JsonNode arr = root.path("data").path("pasca");
                        if (!arr.isArray()) {
                            throw new RuntimeException("Format JSON tidak sesuai: data.pasca bukan array");
                        }
                        List<PostpaidProduct> list = new ArrayList<>();
                        for (JsonNode n : arr) {
                            PostpaidProduct p = new PostpaidProduct();
                            p.setCode(s(n, "code"));
                            p.setName(s(n, "name"));
                            p.setStatus(i(n, "status"));
                            p.setFee(l(n, "fee"));
                            p.setKomisi(l(n, "komisi"));
                            p.setType(s(n, "type"));
                            p.setCategory(s(n, "category"));
                            list.add(p);
                        }
                        return list;
                    } catch (Exception e) {
                        throw new RuntimeException("Gagal parse JSON: " + e.getMessage(), e);
                    }
                });
    }

    public InquiryResponsePostPaid sendInquiryPascaPostPaid(
            String customerNumber, String productCode, String refId, String month, String type, String year, String identitas, String nominal, String billCode, String billKey) {

        try {

            String sign = SignatureUtil.md5Hex(username + apiKey + refId);

            ObjectNode body = mapper.createObjectNode()
                    .put("commands", "inq-pasca")
                    .put("hp", customerNumber)
                    .put("code", productCode)
                    .put("ref_id", refId)
                    .put("username", username)
                    .put("sign", sign);
            if (type != null && type.equalsIgnoreCase("bpjs") && month != null && !month.isBlank()) {
                body.put("month", month);
            }

            if (type != null && type.equalsIgnoreCase("pbb") && year != null && !year.isBlank()) {
                body.put("year", year);
            }

            if (type != null && type.equalsIgnoreCase("pajak-kendaraan") && identitas != null && !identitas.isBlank()) {
                body.put("nomor_identitas", identitas);
            }

            if(type != null && (type.equalsIgnoreCase("emoney")
                    || type.equalsIgnoreCase("dm-member")
                    || type.equalsIgnoreCase("dm-nonmember")) && nominal != null && !nominal.isBlank()) {
                body.putObject("desc")
                        .put("amount", nominal);
            }

            if (type != null && type.equalsIgnoreCase("pendidikan")
                    && billCode != null && billKey != null) {
                com.fasterxml.jackson.databind.node.ObjectNode desc = body.putObject("desc");
                desc.put("biller_code", billCode);
                desc.put("bill_key", billKey);
            }


            HttpRequest req = HttpRequest.newBuilder(URI.create(urlPostPaid + "/api/v1/bill/check"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (resp.statusCode() / 100 != 2) {
                throw new RuntimeException("HTTP " + resp.statusCode() + ": " + resp.body());
            }

            return mapper.readValue(resp.body(), InquiryResponsePostPaid.class);

        } catch (Exception e) {
            throw new RuntimeException("sendInquiryPascaPostPaid failed: " + e.getMessage(), e);
        }
    }

    private static String s(JsonNode n, String f) { return n.hasNonNull(f) ? n.get(f).asText() : null; }
    private static Integer i(JsonNode n, String f) { return n.hasNonNull(f) ? n.get(f).asInt() : null; }
    private static Long l(JsonNode n, String f) { return n.hasNonNull(f) ? n.get(f).asLong() : null; }


    public Long fetchBalanceSync() {
        try {
            String sign = SignatureUtil.md5Hex(username + apiKey + "bl");

            ObjectNode body = mapper.createObjectNode();
            body.put("commands", "balance");
            body.put("username", username);
            body.put("sign", sign);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(urlPrePaid + "/v1/legacy/index"))
                    .timeout(Duration.ofSeconds(25))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();


            var http = java.net.http.HttpClient.newHttpClient();
            var resp = http.send(req, java.net.http.HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));

            if (resp.statusCode() / 100 != 2) {
                throw new RuntimeException("HTTP " + resp.statusCode() + ": " + resp.body());
            }

            String raw = resp.body();
            var root = mapper.readTree(raw);
            return root.path("data").path("balance").asLong(0L);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public InquiryResponsePostPaid payPascaSync(PostpaidProduct item, InquiryResponsePostPaid r) {
        try {

            String sign = SignatureUtil.md5Hex(username + apiKey + r.data.tr_id);

            ObjectNode body = mapper.createObjectNode()
                    .put("commands", "pay-pasca")
                    .put("username", username)
                    .put("tr_id", r.data.tr_id)
                    .put("sign", sign);


            HttpRequest req = HttpRequest.newBuilder(URI.create(urlPostPaid + "/api/v1/bill/check"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (resp.statusCode() / 100 != 2) {
                throw new RuntimeException("HTTP " + resp.statusCode() + ": " + resp.body());
            }

            return mapper.readValue(resp.body(), InquiryResponsePostPaid.class);

        } catch (Exception e) {
            throw new RuntimeException("sendPayment failed: " + e.getMessage(), e);
        }
    }

    public InquiryResponsePostPaid checkStatusPostPaid(String refId) {
        try {
            String sign = SignatureUtil.md5Hex(username + apiKey + "cs");

            ObjectNode body = mapper.createObjectNode()
                    .put("commands", "checkstatus")
                    .put("username", username)
                    .put("ref_id", refId)
                    .put("sign", sign);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(urlPostPaid + "/api/v1/bill/check"))
                    .timeout(Duration.ofSeconds(25))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new RuntimeException("HTTP " + resp.statusCode() + " : " + resp.body());
            }

            InquiryResponsePostPaid out = mapper.readValue(resp.body(), InquiryResponsePostPaid.class);
            return out;

        } catch (Exception e) {
            throw new RuntimeException("Topup error: " + e.getMessage(), e);
        }
    }
}
