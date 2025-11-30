package com.babsnet.posapp.util;

import com.babsnet.posapp.model.PrepaidTopupResponse;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ProcessLoading {

    private static final int POLL_INTERVAL_MS = 2000;      // 2 detik
    private static final int DEFAULT_TIMEOUT_SEC = 60;     // kalau mau tanpa batas, ganti jadi -1

    private ProcessLoading() {}

    /** Modeless (tidak mengunci window lain). */
    public static void showProcessingAndWaitSimple(ApiClient api,
                                                   String refId,
                                                   Window ownerWindow) {
        showProcessingAndWaitSimple(api, refId, ownerWindow, Duration.ofSeconds(DEFAULT_TIMEOUT_SEC));
    }

    public static void showProcessingAndWaitSimple(ApiClient api,
                                                   String refId,
                                                   Window ownerWindow,
                                                   Duration overallTimeout) {

        // === Dialog kecil yang TIDAK modal ===
        Stage dialog = new Stage();
        if (ownerWindow != null) dialog.initOwner(ownerWindow);
        dialog.initModality(Modality.NONE);                    // <-- kuncinya: NON-modal
        dialog.initStyle(StageStyle.UTILITY);                  // tampilan mungil
        dialog.setAlwaysOnTop(true);                           // opsional: tetap di depan
        dialog.setTitle("Processing…");

        Label label = new Label("Memproses transaksi…");
        label.setMinWidth(320);
        label.setWrapText(true);

        ProgressIndicator spinner = new ProgressIndicator();
        Button btnCancel = new Button("Batalkan");

        VBox box = new VBox(12, label, spinner, btnCancel);
        box.setPadding(new Insets(16));
        dialog.setScene(new Scene(box));
        dialog.setResizable(false);

        // === Worker thread untuk polling ===
        AtomicBoolean cancelled = new AtomicBoolean(false);

        final boolean noTimeout = overallTimeout == null || overallTimeout.isNegative();
        final long deadline = noTimeout ? Long.MAX_VALUE
                : System.currentTimeMillis() + Math.max(1000, overallTimeout.toMillis());

        Thread worker = new Thread(() -> {
            int attempt = 0;

            while (!cancelled.get() && System.currentTimeMillis() < deadline) {
                attempt++;
                final int att = attempt;
                Platform.runLater(() ->
                        label.setText("Menunggu konfirmasi provider… (" + att + ")"));

                PrepaidTopupResponse check;
                try {
                    check = api.checkStatusTopup(refId);
                } catch (Exception ex) {
                    check = null; // error jaringan sementara -> coba lagi
                }

                if (check != null && check.isSuccess()) {
                    PrepaidTopupResponse ok = check;
                    Platform.runLater(() -> {
                        dialog.close();
                        MessageDialogUtil.showInfo("OK, TRX " + ok.data.trId + " SN=" + ok.data.sn);
                        // TODO: simpan ke DB digital_tx, tampilkan SN/Token di nota
                    });
                    return;
                }

                if (check != null && !isStillProcessing(check)) {
                    String errMsg = safeMsg(check);
                    Platform.runLater(() -> {
                        dialog.close();
                        MessageDialogUtil.showError(errMsg);
                    });
                    return;
                }

                try {
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    break;
                }
            }

            if (cancelled.get()) {
                Platform.runLater(() -> {
                    dialog.close();
                    MessageDialogUtil.showError("Dibatalkan oleh pengguna.");
                });
            } else if (!noTimeout) {
                Platform.runLater(() -> {
                    dialog.close();
                    MessageDialogUtil.showError("Timeout menunggu status final");
                });
            }
            // kalau noTimeout dan keluar loop bukan karena cancel, berarti sudah return SUCCESS/FAIL di atas
        }, "poll-check-status-modeless");

        // tombol batal & X-close window = batalkan polling
        btnCancel.setOnAction(e -> {
            btnCancel.setDisable(true);
            label.setText("Membatalkan…");
            cancelled.set(true);
            worker.interrupt();
        });
        dialog.setOnCloseRequest(e -> {
            cancelled.set(true);
            worker.interrupt();
        });

        // tampilkan TIDAK blocking window utama
        dialog.show();

        worker.setDaemon(true);
        worker.start();
    }

    // ==== util ====
    private static boolean isStillProcessing(PrepaidTopupResponse r) {
        if (r == null || r.data == null) return false;
        String msg = r.data.message == null ? "" : r.data.message.trim();
        return "PROCESS".equalsIgnoreCase(msg)
                || "PROCESSING".equalsIgnoreCase(msg)
                || r.data.status == 2;
    }

    private static String safeMsg(PrepaidTopupResponse r) {
        if (r == null || r.data == null) return "Transaksi gagal";
        String base = (r.data.message == null || r.data.message.isBlank())
                ? "Transaksi gagal" : r.data.message;
        if (r.data.rc != null && !r.data.rc.isBlank()) {
            base += " (RC=" + r.data.rc + ")";
        }
        return base;
    }

}
