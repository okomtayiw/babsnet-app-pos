package com.babsnet.posapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class PlnInquiryResponse {

    @JsonProperty("data")
    public Data data;

    /** SUCCESS jika rc == "00" dan status == 1 */
    public boolean isSuccess() {
        return data != null
                && "00".equals(data.rc)
                && Integer.valueOf(1).equals(data.getStatusInt()); // <-- status 1 = sukses
    }

    /** PENDING hanya untuk sinyal "masih proses" dari provider (bukan status==2) */
    public boolean isPending() {
        if (data == null) return false;
        String msg = data.message == null ? "" : data.message.trim();
        return "39".equals(data.rc)                              // PROCESS
                || "PROCESS".equalsIgnoreCase(msg)
                || "PROCESSING".equalsIgnoreCase(msg);
        // <-- status==2 TIDAK dianggap pending lagi
    }

    /** FAILED = bukan success dan bukan pending (termasuk status==2) */
    public boolean isFailed() {
        return data != null && !isSuccess() && !isPending();
        // jika status==2 => true, karena bukan success dan bukan pending
    }

    /** Helper opsional: cek eksplisit "gagal karena status==2" */
    public boolean isExplicitFailedByStatus() {
        return data != null && Integer.valueOf(2).equals(data.getStatusInt());
    }

    @Override public String toString() { return "PlnInquiryResponse{data=" + data + '}'; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Data {
        @JsonProperty("status")
        public String status;

        @JsonProperty("customer_id")
        public String customerId;

        @JsonProperty("meter_no")
        public String meterNo;

        @JsonProperty("subscriber_id")
        public String subscriberId;

        @JsonProperty("name")
        public String name;

        @JsonProperty("segment_power")
        public String segmentPower;

        @JsonProperty("message")
        public String message;

        @JsonProperty("rc")
        public String rc;

        /** parsing aman ke Integer; null jika kosong/invalid */
        public Integer getStatusInt() {
            if (status == null || status.isBlank()) return null;
            try { return Integer.parseInt(status.trim()); }
            catch (NumberFormatException ignore) { return null; }
        }

        @Override public String toString() {
            return "Data{status=" + status +
                    ", customerId='" + customerId + '\'' +
                    ", meterNo='" + meterNo + '\'' +
                    ", subscriberId='" + subscriberId + '\'' +
                    ", name='" + name + '\'' +
                    ", segmentPower='" + segmentPower + '\'' +
                    ", message='" + message + '\'' +
                    ", rc='" + rc + '\'' +
                    '}';
        }
    }
}
