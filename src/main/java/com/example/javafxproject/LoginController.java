package com.example.javafxproject;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    private static String bearerToken;

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = usernameField.getText();
        String password = passwordField.getText();
        HttpClient httpClient = HttpClient.newHttpClient();
        String requestBody = "email=" + email + "&password=" + password;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                String userId = jsonNode.get("userId").asText();

                bearerToken = jsonNode.get("accessToken").asText();

//                        response.headers().firstValue("Authorization").orElse(null);
//                System.out.println("Bearer Token" + bearerToken);
//                System.out.println("User ID: " + userId);
                String orgApiUrl = "http://localhost:8080/api/organization/getOrgId/" + userId;
                HttpRequest orgRequest = HttpRequest.newBuilder()
                        .uri(URI.create(orgApiUrl))
                        .GET()
                        .build();
                HttpResponse<String> orgResponse = httpClient.send(orgRequest, HttpResponse.BodyHandlers.ofString());
                if (orgResponse.statusCode() == 200) {
                    String orgResponseBody = orgResponse.body();
                    JsonNode orgJsonNode = objectMapper.readTree(orgResponseBody);
                    int organizationId = orgJsonNode.get("data").asInt();
                    System.out.println("Organization ID response: " + orgResponseBody);
                    System.out.println("Organization ID: " + organizationId);
                    try (Connection connection = DatabaseManager.connect()) {
                        String createTableSQL = "CREATE TABLE IF NOT EXISTS login_details (org_id BIGINT,user_id BIGINT,access_token TEXT)";
                        try (PreparedStatement createTableStatement = connection.prepareStatement(createTableSQL)) {
                            createTableStatement.executeUpdate();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        String insertMemberDataSQL = "INSERT INTO login_details (org_id ,user_id,access_token) VALUES (?,?,?)";
                        try (PreparedStatement insertStatement = connection.prepareStatement(insertMemberDataSQL)) {
                            insertStatement.setInt(1, organizationId);
                            insertStatement.setInt(2, Integer.parseInt(userId));
                            insertStatement.setString(3, bearerToken);

                            insertStatement.executeUpdate();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    System.out.println("Failed to get Organization ID. Status code: " + orgResponse.statusCode());
                }
                Stage stage = (Stage) usernameField.getScene().getWindow();
                stage.close();
                openMainPage();
            } else {
                showInvalidCredentialsAlert();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showInvalidCredentialsAlert() {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Invalid Credentials");
        alert.setHeaderText(null);
        alert.setContentText("The username or password is incorrect. Please try again.");
        alert.showAndWait();
    }

    private void openMainPage() {
        try {
            FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("attendance-view.fxml"));
            Parent root = mainLoader.load();
            AttendanceApplicationController attendanceApplicationController = mainLoader.getController();

            attendanceApplicationController.setBearerToken(bearerToken);

            Scene mainScene = new Scene(root, 1280, 650);
            Stage primaryStage = new Stage();
            primaryStage.setTitle("Attendance Master");
            primaryStage.setScene(mainScene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public static String sendBearerToken() {
//        return bearerToken;
//    }
}
