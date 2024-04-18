package com.example.javafxproject;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AttendanceApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader splashLoader = new FXMLLoader(AttendanceApplication.class.getResource("splashscreen-view.fxml"));
        Parent splashRoot = splashLoader.load();
        Scene splashScene = new Scene(splashRoot);
        Stage splashStage = new Stage();
        splashStage.initStyle(StageStyle.UNDECORATED);
        splashStage.setScene(splashScene);
        splashStage.setMaximized(true);
//        splashStage.show();
        PauseTransition pause = new PauseTransition(Duration.seconds(4));
        pause.setOnFinished(event -> {
            splashStage.close();
            loadMainView(stage);
        });
        pause.play();
    }

    private void loadMainView(Stage stage) {
        String userId = getUserId();

        FXMLLoader fxmlLoader;
        if (userId != null && !userId.isEmpty() && isUserIdExists(userId)) {
            fxmlLoader = new FXMLLoader(AttendanceApplication.class.getResource("attendance-view.fxml"));
        } else {
            fxmlLoader = new FXMLLoader(AttendanceApplication.class.getResource("loginpop-view.fxml"));
        }
        try {
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root);
            stage.setMaximized(true);
            stage.setTitle("Attendify");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getUserId() {
        try (Connection connection = DatabaseManager.connect()) {
            String selectUserIdSQL = "SELECT org_id FROM login_details LIMIT 1";
            try (PreparedStatement selectUserIdStatement = connection.prepareStatement(selectUserIdSQL);
                 ResultSet resultSet = selectUserIdStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("org_id");
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isUserIdExists(String userId) {
        try (Connection connection = DatabaseManager.connect()) {
            String checkUserIdSQL = "SELECT org_id FROM login_details WHERE org_id = ? LIMIT 1";
            try (PreparedStatement checkUserIdStatement = connection.prepareStatement(checkUserIdSQL)) {
                checkUserIdStatement.setString(1, userId);
                try (ResultSet resultSet = checkUserIdStatement.executeQuery()) {
                    return resultSet.next();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
