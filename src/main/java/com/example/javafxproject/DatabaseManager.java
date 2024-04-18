package com.example.javafxproject;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DatabaseManager {
    private static final String DATABASE_URL = "jdbc:sqlite:attendify.db";

    static {
        try {
            Connection connection = connect();
            createTable(connection);
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Connection connect() throws SQLException {
        createDatabaseIfNotExists();
        return DriverManager.getConnection(DATABASE_URL);
    }

    private static void createTable(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            String dropTableSQL = "DROP TABLE IF EXISTS user_id";
            statement.execute(dropTableSQL);
            String createTableSQL = "CREATE TABLE IF NOT EXISTS attendance ("
                    + "member_id BIGINT,"
                    + "member_name TEXT,"
                    + "attendance_marked_at TEXT)";
            statement.execute(createTableSQL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createDatabaseIfNotExists() {

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String databaseName = "attendify_" + timestamp + ".db";
        File databaseFile = new File(databaseName);
        if (databaseFile.exists()) {
            try {
                boolean deleted = databaseFile.delete();
                if (!deleted) {
                    System.err.println("Failed to delete the old database file.");
                } else {
                    System.out.println("Old database file deleted successfully.");
                }
            } catch (SecurityException e) {
                System.err.println("SecurityException: " + e.getMessage());
            }
        }
        createDatabase();
    }

    private static void createDatabase() {
    }
}
