package com.example.javafxproject;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;


//import javax.swing.text.html.ImageView;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AttendanceApplicationController implements Initializable {

    private ObservableList<MemberData> memberData;
    @FXML
    private Label timeLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private TableView<MemberData> tableView;
    @FXML
    private TableView<MemberData> tableView1;
    @FXML
    private TextField searchTextField;
    @FXML
    private ChoiceBox<String> myChoiceBox;
    @FXML
    private Button sendDataButton;
    @FXML
    private Button logoutButton;
    private String[] item = {"Member ID", "Member Name", "Phone.No"};
    private static final String HEALTH_CHECK_URL = "http://localhost:8080/actuator/health";
    private static final long CHECK_INTERVAL_SECONDS = 10;
    private ScheduledExecutorService scheduler;
    private boolean isInternetAvailable;
    private static String bearerToken;
    @FXML
    private ImageView statusImageView;
    @FXML
    private Text statusText;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            String orgId = fetchOrganizationId();
            System.out.println("Wow " + bearerToken);
            System.out.println("Wow " + bearerToken);

            URL memberUrl = new URL("http://localhost:8080/api/member/getAll/" + orgId);
            HttpURLConnection connection = (HttpURLConnection) memberUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
            int responseCode = connection.getResponseCode();
            System.out.println("HTTP Response Code: " + responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK) {
                memberData = fetchMemberDataFromAPI(connection, "name", "phoneNumber");
                checkConnection();
                if (isInternetAvailable()) {
                    sendDataButton.setDisable(false);
                } else {
                    sendDataButton.setDisable(true);
                }
            } else {
                System.err.println("Failed to fetch data from API. HTTP response code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        scheduler = Executors.newSingleThreadScheduledExecutor();
        isInternetAvailable = true;

        startChecking();
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            Date now = new Date();
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
            timeLabel.setText(timeFormat.format(now));
            dateLabel.setText(dateFormat.format(now));
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        checkConnection();
        initializeTableColumns();
        initializeChoiceBox();
        fetchDataForTableView1();
        displayMemberData();

        // Initialize the search functionality
        initializeSearch();

        sendDataButton.setOnAction(event ->
        {
            initializeSearch();
            saveMemberDataToLocalDatabase(memberData);
            sendDataToBackend();
            deleteAllDataFromAttendanceTable();
            fetchDataForTableView1();
            displayMemberData();
        });

        logoutButton.setOnAction(event ->
        {
            handleLogout();
        });
    }

    public void setBearerToken(String token) {
        this.bearerToken = token;
//        System.out.println("Bearer Token Inside the Attendance Application Controller " + bearerToken);
//        System.out.println("Token Inside the Attendance Application Controller " + token);
    }

    private void startChecking() {
        scheduler.scheduleAtFixedRate(this::checkConnection, 0, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void checkConnection() {
        try {
            URL actuatorURL = new URL(HEALTH_CHECK_URL);
            HttpURLConnection connection = (HttpURLConnection) actuatorURL.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            boolean wasInternetAvailable = isInternetAvailable;

            if (responseCode == HttpURLConnection.HTTP_OK) {
                isInternetAvailable = true;
            } else {
                isInternetAvailable = false;
            }

            connection.disconnect();

            if (wasInternetAvailable != isInternetAvailable) {
                Platform.runLater(() -> updateButtonStatus());
            }
        } catch (Exception e) {
            isInternetAvailable = false;
            Platform.runLater(() -> updateButtonStatus());
            e.printStackTrace();
        }
    }

    public boolean isInternetAvailable() {
        return isInternetAvailable;
    }

    private void updateButtonStatus() {
        /*if (isInternetAvailable) {
            sendDataButton.setDisable(false);
//            statusImageView.setImage(new Image("file:///path/to/online.png"));

//            statusImageView.setImage(new Image("@../../../images/online.png"));
//            statusImageView.setImage(new Image(getClass().getResource("/images/online.png").toExternalForm()));

//            statusText.setText("Online");
//            statusText.setFill(Color.GREEN);
        } else {
            sendDataButton.setDisable(true);
//            statusImageView.setImage(new Image("file:///path/to/offline.png"));

//            statusImageView.setImage(new Image("@../../../images/offline.png"));
//            statusImageView.setImage(new Image(getClass().getResource("/images/offline.png").toExternalForm()));

//            statusText.setText("Offline");
//            statusText.setFill(Color.GREY);
        }*/
    }

    private String fetchOrganizationId() {
        String orgId = null;
        try (Connection connection = DatabaseManager.connect()) {
            String selectQuery = "SELECT org_id,access_token FROM login_details LIMIT 1";
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(selectQuery)) {

                if (resultSet.next()) {
                    orgId = String.valueOf(resultSet.getInt("org_id"));
                    bearerToken = resultSet.getString("access_token");
                    System.out.println("Organization ID: " + orgId);
                } else {
                    System.out.println("No data found in the organization table.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println(orgId);
        return orgId;
    }

    @FXML
    private void sendDataToBackend() {
        displayMemberData();
        initializeSearch();
        saveMemberDataToLocalDatabase(memberData);
        List<AttendanceRequest> data = new ArrayList<>();
        List<MemberData> attendanceDataList = retrieveAttendanceDataFromSQLite();

        for (MemberData attendanceData : attendanceDataList) {
            AttendanceRequest attendanceRequest = new AttendanceRequest();
            attendanceRequest.setId(attendanceData.getMemberId());
            String localDateTime = attendanceData.getAttendanceMarkedAt();
            attendanceRequest.setAttendanceMarkedAt(localDateTime);
            data.add(attendanceRequest);
        }
        try {
            String orgId = fetchOrganizationId();
            URL backendUrl = new URL("http://localhost:8080/api/attendance/list/" + orgId);
            System.out.println("Sending data to the backend");
            sendPostRequestToBackend(backendUrl, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        Stage currentStage = (Stage) logoutButton.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("logoutpop-view.fxml"));
            Parent root = loader.load();
            LogoutController logoutController = loader.getController();
            logoutController.setMainStage(currentStage);
            Scene scene = new Scene(root);
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Logout");
            Stage mainStage = (Stage) logoutButton.getScene().getWindow();
            stage.initOwner(mainStage);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteAllDataFromAttendanceTable() {
        try (Connection connection = DatabaseManager.connect()) {
            String deleteSQL = "DELETE FROM attendance";
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(deleteSQL);
            }
            fetchDataForTableView1();
            tableView1.refresh();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void sendPostRequestToBackend(URL url, List<AttendanceRequest> data) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            JSONObject jsonObject = new JSONObject();
            JSONArray list = new JSONArray();

            for (AttendanceRequest request : data) {
                JSONObject attendance = new JSONObject();
                attendance.put("id", request.getId());
                attendance.put("attendanceMarkedAt", request.getAttendanceMarkedAt());
                list.add(attendance);
            }
            jsonObject.put("attendanceDataList", list);
            System.out.println("Sending JSON payload: " + jsonObject.toString());
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(jsonObject.toString().getBytes());
                outputStream.flush();
            }
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("Data sent to the backend successfully.");
            } else {
                System.err.println("Failed to send data to the backend. HTTP response code: " + responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private ObservableList<MemberData> fetchMemberDataFromAPI(HttpURLConnection connection, String... columns) throws Exception {
        int responseCode = connection.getResponseCode();
        System.out.println("HTTP Response Code: " + responseCode);

        if (responseCode == HttpURLConnection.HTTP_OK) {
            Scanner scanner = new Scanner(connection.getInputStream());
            StringBuilder response = new StringBuilder();
            while (scanner.hasNext()) {
                response.append(scanner.nextLine());
            }
            scanner.close();
            JSONArray jsonArray = (JSONArray) new JSONParser().parse(response.toString());
            ObservableList<MemberData> memberDataList = FXCollections.observableArrayList();

            for (Object jsonElement : jsonArray) {
                JSONObject jsonObject = (JSONObject) jsonElement;
                Long memberId = Long.valueOf(String.valueOf(jsonObject.get("id")));
                String memberName = (String) jsonObject.get("name");
                String phoneNumber = (String) jsonObject.get("phoneNumber");
                MemberData member = new MemberData(memberId, memberName, phoneNumber);
                Platform.runLater(() -> memberDataList.add(member));
            }

            System.out.println("Fetched Member Data: " + memberDataList);
            return memberDataList;
        } else {
            throw new RuntimeException("Failed to fetch data from API. HTTP response code: " + responseCode);
        }
    }

    private void initializeTableColumns() {
        TableColumn<MemberData, String> memberIdColumn = new TableColumn<>("Member Id");
        TableColumn<MemberData, String> memberNameColumn = new TableColumn<>("Member Name");
        TableColumn<MemberData, String> phoneColumn = new TableColumn<>("Phone No");
        TableColumn<MemberData, Void> markAttendanceColumn = new TableColumn<>("Attendance");

        memberIdColumn.setCellValueFactory(new PropertyValueFactory<>("memberId"));
        memberNameColumn.setCellValueFactory(new PropertyValueFactory<>("memberName"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));

        TableColumn<MemberData, String> memberIdColumn2 = new TableColumn<>("Member Id");
        TableColumn<MemberData, String> memberNameColumn2 = new TableColumn<>("Member Name");
        TableColumn<MemberData, String> timeColumn = new TableColumn<>("Time");
        TableColumn<MemberData, String> actionsColumn = new TableColumn<>("Actions");

        memberIdColumn2.setCellValueFactory(new PropertyValueFactory<>("id"));
        timeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedLocalDate()));
        memberNameColumn2.setCellValueFactory(new PropertyValueFactory<>("memberName"));
        actionsColumn.setCellValueFactory(new PropertyValueFactory<>("actions"));

        memberIdColumn.setSortable(false);
        memberNameColumn.setSortable(false);
        phoneColumn.setSortable(false);
        markAttendanceColumn.setSortable(false);
        memberIdColumn2.setSortable(false);
        timeColumn.setSortable(false);
        actionsColumn.setSortable(false);
        memberNameColumn2.setSortable(false);

        tableView.getColumns().addAll(memberIdColumn, memberNameColumn, phoneColumn, markAttendanceColumn);
        tableView1.getColumns().addAll(memberIdColumn2, memberNameColumn2, timeColumn, actionsColumn);

        actionsColumn.setCellFactory(column -> {
            return new TableCell<MemberData, String>() {
                private final Label actionsLabel = new Label("\u2026");

                {
                    actionsLabel.setStyle("-fx-cursor: hand; -fx-font-size: 18;");
                    actionsLabel.setOnMouseClicked(event -> {
                        MemberData rowData = getTableView().getItems().get(getIndex());
                        ContextMenu contextMenu = new ContextMenu();
                        MenuItem deleteMenuItem = createStyledMenuItem("Delete", 12);
                        MenuItem markUnmarkedMenuItem = createStyledMenuItem("Mark Unmarked", 12);
                        deleteMenuItem.setOnAction(deleteEvent -> {
                            deleteRow(rowData);
                        });

                        markUnmarkedMenuItem.setOnAction(markUnmarkedEvent -> {
                            System.out.println("Mark Unmarked action for: " + rowData.getMemberName());
                        });
                        contextMenu.getItems().addAll(deleteMenuItem, markUnmarkedMenuItem);
                        contextMenu.show(actionsLabel, event.getScreenX(), event.getScreenY());
                    });
                }

                private MenuItem createStyledMenuItem(String text, int fontSize) {
                    MenuItem menuItem = new MenuItem(text);
                    menuItem.setStyle("-fx-font-size: " + fontSize + "; -fx-padding: 5;");
                    return menuItem;
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                    } else {
                        setGraphic(actionsLabel);
                    }
                }
            };
        });

        double columnWidth = 140.0;
        memberIdColumn.setPrefWidth(columnWidth);
        memberNameColumn.setPrefWidth(columnWidth);
        phoneColumn.setPrefWidth(columnWidth);

        tableView.getColumns().addAll(memberIdColumn, memberNameColumn, phoneColumn, markAttendanceColumn);
        tableView1.getColumns().addAll(memberIdColumn2, timeColumn, actionsColumn);

        memberIdColumn.setStyle("-fx-background-color: white; -fx-alignment: CENTER; -fx-border-color: transparent;");
        memberIdColumn2.setStyle("-fx-background-color: white; -fx-alignment: CENTER; -fx-border-color: transparent;");
        memberNameColumn.setStyle("-fx-background-color: white; -fx-alignment: CENTER; -fx-border-color: transparent;");
        phoneColumn.setStyle("-fx-background-color: white; -fx-alignment: CENTER; -fx-border-color: transparent;");
        timeColumn.setStyle("-fx-background-color: white; -fx-alignment: CENTER; -fx-border-color: transparent;");
        markAttendanceColumn.setStyle("-fx-background-color: white; -fx-alignment: CENTER; -fx-border-color: transparent;");
        actionsColumn.setStyle("-fx-background-color: white; -fx-alignment: CENTER; -fx-border-color: transparent;");
        memberNameColumn2.setStyle("-fx-background-color: white; -fx-alignment: CENTER; -fx-border-color: transparent;");

        tableView.setRowFactory(row -> {
            TableRow<MemberData> tableRow = new TableRow<>();
            tableRow.setStyle("-fx-background-color: white; -fx-border-width: 0 0 1 0; -fx-border-color: #e0e0e0; ");
            return tableRow;
        });

        tableView1.setRowFactory(row -> {
            TableRow<MemberData> tableRow = new TableRow<>();
            tableRow.setStyle("-fx-background-color: white; -fx-border-width: 0 0 1 0; -fx-border-color: #e0e0e0; ");
            return tableRow;
        });

        markAttendanceColumn.setCellFactory(param -> new TableCell<>() {
            private final Button markAttendanceButton = new Button("Mark Attendance");

            {
                tableView1.refresh();
                fetchDataForTableView1();

                markAttendanceButton.setOnAction(event -> {
                    tableView1.refresh();
                    fetchDataForTableView1();
                    MemberData rowData = getTableView().getItems().get(getIndex());
                    System.out.println("Mark attendance for: " + rowData.getName());
                    markAttendanceButton.setText("Marked");
                    markAttendanceButton.setDisable(true);
                    markAttendanceButton.setStyle("-fx-background-color: white; -fx-text-fill: #61bdd4; -fx-font-weight: bolder; -fx-font-size: 14;");
                    tableView1.refresh();
                    try (Connection connection = DatabaseManager.connect()) {
                        LocalDateTime attendance_marked_at = LocalDateTime.now();
                        String sql = "INSERT INTO attendance (member_id,member_name,attendance_marked_at) VALUES (?, ?, ?)";
                        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                            preparedStatement.setLong(1, rowData.getMemberId());
                            preparedStatement.setString(2, rowData.getMemberName());
                            preparedStatement.setString(3, attendance_marked_at.toString());
                            preparedStatement.executeUpdate();
                            tableView1.refresh();
                            fetchDataForTableView1();
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(markAttendanceButton);
                    if ("Marked".equals(markAttendanceButton.getText())) {
                        markAttendanceButton.setStyle("-fx-background-color: white; -fx-text-fill: #61bdd4; -fx-font-weight: bolder; -fx-font-size: 14;");
                    } else {
                        markAttendanceButton.setStyle("-fx-background-color: #00008B; -fx-text-fill: white; -fx-font-weight: bolder; -fx-font-size: 14;");
                    }
                }
            }
        });
    }

    private void deleteRow(MemberData rowData) {
        try (Connection connection = DatabaseManager.connect()) {
            String deleteSQL = "DELETE FROM members_data WHERE member_id = ?";
            try (PreparedStatement deleteStatement = connection.prepareStatement(deleteSQL)) {
                deleteStatement.setLong(1, rowData.getMemberId());
                int affectedRows = deleteStatement.executeUpdate();
                if (affectedRows > 0) {
                    System.out.println("Row deleted successfully.");
                    fetchDataForTableView1();
                } else {
                    System.err.println("Failed to delete row. No rows affected.");
                }

                String deleteAttendanceSQL = "DELETE FROM attendance WHERE member_id = ?";
                try (PreparedStatement deleteAttendanceStatement = connection.prepareStatement(deleteAttendanceSQL)) {
                    deleteAttendanceStatement.setLong(1, rowData.getMemberId());
                    deleteAttendanceStatement.executeUpdate();
                }
                fetchDataForTableView1();
            }
            fetchDataForTableView1();
        } catch (SQLException e) {
            e.printStackTrace();
            fetchDataForTableView1();
        }
        fetchDataForTableView1();
    }

    @FXML
    private void fetchDataForTableView1() {
        List<MemberData> attendanceDataList = retrieveAttendanceDataFromSQLite();
        tableView1.getItems().clear();
        tableView1.getItems().addAll(attendanceDataList);
    }

    private List<MemberData> retrieveAttendanceDataFromSQLite() {
        List<MemberData> attendanceDataList = new ArrayList<>();
        try (Connection connection = DatabaseManager.connect()) {
            String sql = "SELECT * FROM attendance";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
                 ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    Long memberId = resultSet.getLong("member_id");
                    String memberName = resultSet.getString("member_name");
                    String attendanceMarkedAt = resultSet.getString("attendance_marked_at");
                    MemberData rowData = new MemberData();
                    rowData.setId(memberId);
                    rowData.setMemberName(memberName);
                    rowData.setAttendanceMarkedAt(attendanceMarkedAt);
                    attendanceDataList.add(rowData);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        attendanceDataList.sort(Comparator.comparing(MemberData::getAttendanceMarkedAt));
        return attendanceDataList;
    }

    private void displayMemberData() {
        System.out.println("Inside the Display Member Data");
        try (Connection connection = DatabaseManager.connect()) {
            String sql = "SELECT * FROM members_data";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
                 ResultSet resultSet = preparedStatement.executeQuery()) {
                ObservableList<MemberData> memberDataList = FXCollections.observableArrayList();

                while (resultSet.next()) {
                    Long memberId = resultSet.getLong("member_id");
                    String memberName = resultSet.getString("member_name");
                    String phoneNumber = resultSet.getString("phone_number");
                    MemberData memberData = new MemberData(memberId, memberName, phoneNumber);
                    memberDataList.add(memberData);
                }

                // Set the member data to the TableView
                tableView.setItems(memberDataList);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveMemberDataToLocalDatabase(List<MemberData> memberDataList) {
        try {
            try (Connection connection = DatabaseManager.connect()) {
                String createTableSQL = "CREATE TABLE IF NOT EXISTS members_data ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "member_id BIGINT UNIQUE,"
                        + "member_name VARCHAR(255),"
                        + "phone_number VARCHAR(20))";
                try (PreparedStatement createTableStatement = connection.prepareStatement(createTableSQL)) {
                    createTableStatement.executeUpdate();
                }
                String insertDataSQL = "INSERT INTO members_data (member_id, member_name, phone_number) VALUES (?, ?, ?)";
                try (PreparedStatement insertDataStatement = connection.prepareStatement(insertDataSQL)) {
                    for (MemberData memberData : memberDataList) {
                        insertDataStatement.setLong(1, memberData.getMemberId());
                        insertDataStatement.setString(2, memberData.getMemberName());
                        insertDataStatement.setString(3, memberData.getPhone());
                        insertDataStatement.executeUpdate();

                        String insertAttendanceSQL = "INSERT INTO attendance (member_id, member_name, attendance_marked_at) VALUES (?, ?,?)";
                        try (PreparedStatement insertAttendanceStatement = connection.prepareStatement(insertAttendanceSQL)) {
                            insertAttendanceStatement.setLong(1, memberData.getMemberId());
                            insertAttendanceStatement.setString(2, memberData.getName());
                            insertAttendanceStatement.setString(3, LocalDateTime.now().toString());
                            insertAttendanceStatement.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initializeChoiceBox() {
        myChoiceBox.getItems().addAll(item);
        myChoiceBox.setPrefHeight(38);
        myChoiceBox.setPrefWidth(120);
        myChoiceBox.getSelectionModel().select(0);

        myChoiceBox.setOnAction(event -> {
            String selectedCriteria = myChoiceBox.getSelectionModel().getSelectedItem();
            if (selectedCriteria != null) {
                switch (selectedCriteria) {
                    case "Member ID":
                        sortTable(tableView, "id");
                        break;
                    case "Member Name":
                        sortTable(tableView, "name");
                        break;
                    case "Phone.No":
                        sortTable(tableView, "phoneNumber");
                        break;
                }
            }
        });
    }

    private void sortTable(TableView<MemberData> tableView, String columnName) {
        TableColumn<MemberData, ?> column = null;
        for (TableColumn<MemberData, ?> col : tableView.getColumns()) {
            if (col.getText().equals(columnName)) {
                column = col;
                break;
            }
        }
        if (column != null) {
            tableView.getSortOrder().clear();
            tableView.getSortOrder().add(column);
        }
    }

    private void initializeSearch() {
        ObservableList<MemberData> tableViewItems = tableView.getItems();
        FilteredList<MemberData> filteredData = new FilteredList<>(tableViewItems, p -> true);

        searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(memberData -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();
                String selectedCriteria = myChoiceBox.getSelectionModel().getSelectedItem();

                switch (selectedCriteria) {
                    case "Member ID":
                        return String.valueOf(memberData.getMemberId()).toLowerCase().contains(lowerCaseFilter);
                    case "Member Name":
                        return memberData.getMemberName().toLowerCase().contains(lowerCaseFilter);
                    case "Phone.No":
                        return memberData.getPhone().toLowerCase().contains(lowerCaseFilter);
                    default:
                        return true;
                }
            });
        });

        // Wrap the filtered list in a SortedList
        SortedList<MemberData> sortedData = new SortedList<>(filteredData);

        // Bind the SortedList comparator to the TableView comparator
        sortedData.comparatorProperty().bind(tableView.comparatorProperty());

        // Set the sorted data to the TableView
        tableView.setItems(sortedData);
    }

}
