package com.example.javafxproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class LogoutController {

    @FXML
    private Button closeApplicationButton;

    @FXML
    private Button logoutButton;

    @FXML
    private Button cancelButton;

    private Stage mainStage;

    public void setMainStage(Stage mainStage) {
        this.mainStage = mainStage;
    }

    @FXML
    private void initialize() {

        closeApplicationButton.setOnAction(event -> {
            closeApplication();
        });

        logoutButton.setOnAction(event -> {
            handleLogout();
        });

        cancelButton.setOnAction(event -> {
            closePopup();
        });
    }

    private void closeApplication() {
        Platform.exit();
    }

    @FXML
    private void handleLogout() {
        try (Connection connection = DatabaseManager.connect()) {
            String deleteUserId = "DELETE FROM login_details";
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(deleteUserId);
            }
            String deleteMembersDataQuery = "DELETE FROM members_data";
            try (Statement membersDataStatement = connection.createStatement()) {
                membersDataStatement.executeUpdate(deleteMembersDataQuery);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (mainStage != null) {
            mainStage.close();
        } else {
            System.out.println("Main stage is null. Unable to close.");
        }
        if (mainStage != null) {
            mainStage.close();
        } else {
            System.out.println("Main stage is null. Unable to close.");
        }
        closeCurrentWindow();
    }

    private void closeCurrentWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("loginpop-view.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);

            Stage loginStage = new Stage();
            loginStage.setScene(scene);
            loginStage.setMaximized(true);
            Stage currentStage = (Stage) logoutButton.getScene().getWindow();
            currentStage.close();
            loginStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closePopup() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
