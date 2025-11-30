package com.babsnet.posapp.model;

import java.time.LocalDate;

public class ExportParams {
    LocalDate start;
    LocalDate end;
    ExportFormatEnum exportFormat;

    public ExportParams(LocalDate start, LocalDate end, ExportFormatEnum exportFormat) {
        this.start = start;
        this.end = end;
        this.exportFormat = exportFormat;
    }

    public LocalDate getStart() {
        return start;
    }

    public void setStart(LocalDate start) {
        this.start = start;
    }

    public LocalDate getEnd() {
        return end;
    }

    public void setEnd(LocalDate end) {
        this.end = end;
    }

    public ExportFormatEnum getExportFormat() {
        return exportFormat;
    }

    public void setExportFormat(ExportFormatEnum exportFormat) {
        this.exportFormat = exportFormat;
    }
}
