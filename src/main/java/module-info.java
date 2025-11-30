module com.babsnet.posapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;
    requires java.desktop;
    requires com.github.librepdf.openpdf;
    requires jbcrypt;
    requires org.mariadb.jdbc;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires jcommander;
    requires com.fasterxml.jackson.annotation;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;

    // ==== EXPORTS ====
    exports com.babsnet.posapp;
    exports com.babsnet.posapp.controller;
    exports com.babsnet.posapp.repository;

    // ==== FXML CONTROLLERS ====
    opens com.babsnet.posapp to javafx.fxml;
    opens com.babsnet.posapp.controller to javafx.fxml;
    opens com.babsnet.posapp.repository to javafx.fxml;

    // ==== important to TABLEVIEW + JACKSON ====
    opens com.babsnet.posapp.model to javafx.base, com.fasterxml.jackson.databind;
}
