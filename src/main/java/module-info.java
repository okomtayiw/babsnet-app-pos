module com.babsnet.posapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics; // biasanya otomatis di-require javafx.controls, tapi keep for sure
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

    exports com.babsnet.posapp;
    exports com.babsnet.posapp.controller;
    exports com.babsnet.posapp.repository;

    opens com.babsnet.posapp to javafx.fxml;
    opens com.babsnet.posapp.controller to javafx.fxml;
    opens com.babsnet.posapp.repository to javafx.fxml;
}

