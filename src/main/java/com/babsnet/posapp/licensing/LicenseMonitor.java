package com.babsnet.posapp.licensing;

import com.babsnet.posapp.session.SessionManager;
import com.babsnet.posapp.util.SceneSwitcher;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public  class LicenseMonitor {
    private static final Logger LOG = Logger.getLogger(LicenseMonitor.class.getName());

    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "LicenseMonitor");
        t.setDaemon(true);
        return t;
    });

    private final Duration interval;
    private final AtomicBoolean reacted = new AtomicBoolean(false);

    public LicenseMonitor(Duration interval) {
        this.interval = interval;
    }

    /** Mulai cek berkala (tanpa mengganggu UI thread). */
    public void start() {
        long periodSec = Math.max(60, interval.toSeconds()); // minimal 1 menit agar aman
        exec.scheduleAtFixedRate(this::checkOnce, periodSec, periodSec, TimeUnit.SECONDS);
    }

    /** Hentikan saat app ditutup. */
    public void stop() {
        exec.shutdownNow();
    }

    private void checkOnce() {
        try {
            LicenseStatus st = LicenseGate.check();
            if (st != LicenseStatus.VALID && reacted.compareAndSet(false, true)) {
                Platform.runLater(() -> {
                    // 1) Info ke user
                    new Alert(Alert.AlertType.ERROR, messageFor(st), ButtonType.OK) {{
                        setHeaderText("Lisensi tidak valid");
                    }}.showAndWait();

                    // 2) Paksa logout (kalau sedang login)
                    try { SessionManager.getInstance().logout(); } catch (Exception ignored) {}

                    // 3) Arahkan ke halaman License
                    SceneSwitcher.showLicenseStage(st);
                });
            }
        } catch (Throwable t) {
            LOG.log(Level.WARNING, "Periodic license check failed", t);
        }
    }

    private static String messageFor(LicenseStatus st) {
        return switch (st) {
            case MISSING    -> "License belum terpasang. Silakan impor file license.";
            case EXPIRED    -> "License sudah kadaluarsa. Hubungi penjual untuk perpanjangan.";
            case MISMATCH   -> "License tidak cocok dengan PC ini (hardware mismatch).";
            case READ_ERROR -> "Gagal membaca license. Periksa file license Anda.";
            case VALID      -> "OK";
        };
    }


    public void watchFileChangesAsync(java.nio.file.Path licPath) {
        exec.submit(() -> {
            try (var watcher = licPath.getParent().getFileSystem().newWatchService()) {
                licPath.getParent().register(watcher,
                        java.nio.file.StandardWatchEventKinds.ENTRY_CREATE,
                        java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY,
                        java.nio.file.StandardWatchEventKinds.ENTRY_DELETE);
                while (!Thread.currentThread().isInterrupted()) {
                    var key = watcher.take();
                    for (var ev : key.pollEvents()) {
                        var changed = (java.nio.file.Path) ev.context();
                        if (changed != null && changed.getFileName().toString().equals(licPath.getFileName().toString())) {
                            checkOnce(); // langsung cek
                        }
                    }
                    key.reset();
                }
            } catch (Exception ignored) {}
        });
    }

}
