module com.example.javafxproject {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires json.simple;
    requires java.sql;


    opens com.example.javafxproject to javafx.fxml;
    exports com.example.javafxproject;
}