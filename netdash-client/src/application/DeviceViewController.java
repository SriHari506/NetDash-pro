package application;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import model.Device;
import model.ApiResponse;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceViewController {

    @FXML private TableView<Device> deviceTable;
    @FXML private TableColumn<Device, String> nameCol;
    @FXML private TableColumn<Device, String> ipCol;
    @FXML private TableColumn<Device, String> statusCol;
    @FXML private TableColumn<Device, String> macCol;
    @FXML private TableColumn<Device, String> interfaceCol;
    @FXML private TableColumn<Device, String> protocolCol;
    @FXML private TableColumn<Device, Void> actionCol;
    @FXML private LineChart<Number, Number> cpuChart;

    @FXML private TextField nameField;
    @FXML private TextField ipField;
    @FXML private TextField hostnameField;
    @FXML private TextField interfaceIpField;
    @FXML private Label loadingLabel;

    private final ObservableList<Device> deviceList = FXCollections.observableArrayList();
    private final String BASE_URL = "http://localhost:8080/api/devices";
    private XYChart.Series<Number, Number> cpuSeries;
    private List<Double> cpuHistory = new ArrayList<>();
    private static final int HISTORY_WINDOW = 60;

    @FXML
    public void initialize() {
        setupTableColumns();
        deviceTable.setItems(deviceList);
        deviceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        cpuSeries = new XYChart.Series<>();
        cpuSeries.setName("CPU Usage");
        cpuChart.getData().add(cpuSeries);

        deviceTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                updateChartForDevice(newSelection);
                cpuHistory.clear();
                populateConfigFields(newSelection);
            }
        });

        addActionButtonsToTable();
        loadDevicesFromAPI();
        startPolling();
    }

    private void setupTableColumns() {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        ipCol.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        macCol.setCellValueFactory(new PropertyValueFactory<>("macAddress"));
        interfaceCol.setCellValueFactory(new PropertyValueFactory<>("interfaceStatus"));
        protocolCol.setCellValueFactory(new PropertyValueFactory<>("protocol"));
    }

    private void loadDevicesFromAPI() {
        Platform.runLater(() -> loadingLabel.setText("🔄 Loading devices..."));
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) response.append(line);
                in.close();

                String json = response.toString();
                Gson gson = new GsonBuilder()
                        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                        .create();
                ApiResponse<List<Device>> apiResponse = gson.fromJson(json, new TypeToken<ApiResponse<List<Device>>>(){}.getType());
                List<Device> devices = apiResponse.getData();

                Platform.runLater(() -> {
                    deviceList.setAll(devices);
                    loadingLabel.setText("✅ Devices Loaded: " + devices.size());
                });

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> loadingLabel.setText("⚠️ Error connecting to backend"));
            }
        }).start();
    }

    @FXML
    private void handleAddDevice() {
        String name = nameField.getText().trim();
        String ip = ipField.getText().trim();

        if (name.isEmpty() || ip.isEmpty()) {
            loadingLabel.setText("⚠️ Enter both Name and IP");
            return;
        }

        Device newDevice = new Device(name, ip, "Online");
        new Thread(() -> {
            try {
                Gson gson = new GsonBuilder()
                        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                        .create();
                String json = gson.toJson(newDevice);
                HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes());
                }

                if (conn.getResponseCode() == 201) {
                    Platform.runLater(() -> {
                        nameField.clear();
                        ipField.clear();
                        loadingLabel.setText("✅ Device added");
                        loadDevicesFromAPI();
                    });
                } else {
                    Platform.runLater(() -> loadingLabel.setText("❌ Failed to add device"));
                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> loadingLabel.setText("⚠️ Error adding device"));
            }
        }).start();
    }

    @FXML
    private void handleUpdateDevice() {
        String name = nameField.getText().trim();
        String ip = ipField.getText().trim();
        Device selectedDevice = deviceTable.getSelectionModel().getSelectedItem();

        if (name.isEmpty() || ip.isEmpty() || selectedDevice == null) {
            loadingLabel.setText("⚠️ Select a device and enter both Name and IP");
            return;
        }

        selectedDevice.setName(name);
        selectedDevice.setIpAddress(ip);

        new Thread(() -> {
            try {
                Gson gson = new GsonBuilder()
                        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                        .create();
                String json = gson.toJson(selectedDevice);
                HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + "/" + selectedDevice.getId()).openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes());
                }

                if (conn.getResponseCode() == 200) {
                    Platform.runLater(() -> {
                        nameField.clear();
                        ipField.clear();
                        loadingLabel.setText("✅ Device updated");
                        loadDevicesFromAPI();
                    });
                } else {
                    Platform.runLater(() -> loadingLabel.setText("❌ Failed to update device"));
                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> loadingLabel.setText("⚠️ Error updating device"));
            }
        }).start();
    }

    private void handleDeleteDevice(Device device) {
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + "/" + device.getId()).openConnection();
                conn.setRequestMethod("DELETE");

                if (conn.getResponseCode() == 200) {
                    Platform.runLater(() -> {
                        deviceList.remove(device);
                        loadingLabel.setText("🗑 Deleted: " + device.getName());
                    });
                } else {
                    Platform.runLater(() -> loadingLabel.setText("❌ Delete failed for " + device.getName()));
                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> loadingLabel.setText("⚠️ Error deleting device"));
            }
        }).start();
    }

    private void handleEditDevice(Device device) {
        Platform.runLater(() -> {
            nameField.setText(device.getName());
            ipField.setText(device.getIpAddress());
            loadingLabel.setText("✏️ Edit Mode: Modify and click Update");
        });
    }

    @FXML
    private void discoverNewDevices() {
        Platform.runLater(() -> loadingLabel.setText("🔄 Discovering devices..."));
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/discover");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) response.append(line);
                in.close();

                String json = response.toString();
                Gson gson = new GsonBuilder()
                        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                        .create();
                ApiResponse<List<Device>> apiResponse = gson.fromJson(json, new TypeToken<ApiResponse<List<Device>>>(){}.getType());
                List<Device> devices = apiResponse.getData();

                Platform.runLater(() -> {
                    deviceList.removeIf(d -> d.getName().equals("Local Router"));
                    deviceList.addAll(devices);
                    loadingLabel.setText("✅ Discovered " + devices.size() + " devices");
                });

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> loadingLabel.setText("⚠️ Error discovering devices"));
            }
        }).start();
    }

    private void addActionButtonsToTable() {
        Callback<TableColumn<Device, Void>, TableCell<Device, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Device, Void> call(final TableColumn<Device, Void> param) {
                return new TableCell<>() {
                    private final Button deleteBtn = new Button("❌");
                    private final Button editBtn = new Button("✏️");
                    private final Button configBtn = new Button("⚙️");

                    {
                        deleteBtn.setOnAction(event -> {
                            Device device = getTableView().getItems().get(getIndex());
                            handleDeleteDevice(device);
                        });

                        editBtn.setOnAction(event -> {
                            Device device = getTableView().getItems().get(getIndex());
                            handleEditDevice(device);
                        });

                        configBtn.setOnAction(event -> {
                            Device device = getTableView().getItems().get(getIndex());
                            // Move configuration logic here to match the no-parameter method
                            handleConfigureDevice();
                        });

                        deleteBtn.getStyleClass().add("delete-button");
                        editBtn.getStyleClass().add("edit-button");
                        configBtn.getStyleClass().add("config-button");
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(new HBox(5, editBtn, configBtn, deleteBtn));
                        }
                    }
                };
            }
        };

        actionCol.setCellFactory(cellFactory);
    }

    private void startPolling() {
        Thread pollThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    Device selectedDevice = deviceTable.getSelectionModel().getSelectedItem();
                    if (selectedDevice != null) {
                        fetchDeviceStatus(selectedDevice);
                    }
                } catch (InterruptedException e) {
                    System.out.println("Polling interrupted.");
                    break;
                }
            }
        });

        pollThread.setDaemon(true);
        pollThread.start();
    }

    private void fetchDeviceStatus(Device device) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/" + device.getId() + "/status");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) response.append(line);
                in.close();

                String json = response.toString();
                Gson gson = new GsonBuilder()
                        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                        .create();
                ApiResponse<Device> apiResponse = gson.fromJson(json, new TypeToken<ApiResponse<Device>>(){}.getType());
                Device updatedDevice = apiResponse.getData();

                Platform.runLater(() -> {
                    int index = deviceList.indexOf(device);
                    if (index >= 0) {
                        deviceList.set(index, updatedDevice);
                        if (device.equals(deviceTable.getSelectionModel().getSelectedItem())) {
                            updateChartForDevice(updatedDevice);
                        }
                    }
                });

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateChartForDevice(Device device) {
        cpuHistory.add(device.getCpuUsage());
        if (cpuHistory.size() > HISTORY_WINDOW) cpuHistory.remove(0);

        cpuSeries.getData().clear();
        long currentTime = System.currentTimeMillis() / 1000;
        for (int i = 0; i < cpuHistory.size(); i++) {
            cpuSeries.getData().add(new XYChart.Data<>(currentTime - (HISTORY_WINDOW - i), cpuHistory.get(i)));
        }
    }

    private void populateConfigFields(Device device) {
        hostnameField.setText(device.getName());
        interfaceIpField.setText(device.getIpAddress());
    }

    @FXML
    private void handleConfigureDevice() {
        Device selectedDevice = deviceTable.getSelectionModel().getSelectedItem();
        if (selectedDevice == null) {
            loadingLabel.setText("⚠️ Select a device to configure");
            return;
        }

        String hostname = hostnameField.getText().trim();
        String interfaceIp = interfaceIpField.getText().trim();
        if (hostname.isEmpty() || interfaceIp.isEmpty()) {
            loadingLabel.setText("⚠️ Enter hostname and interface IP");
            return;
        }

        Map<String, String> config = new HashMap<>();
        config.put("hostname", hostname);
        config.put("interfaceIp", interfaceIp);

        new Thread(() -> {
            try {
                Gson gson = new GsonBuilder()
                        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                        .create();
                String json = gson.toJson(config);
                HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + "/" + selectedDevice.getId() + "/config").openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes());
                }

                if (conn.getResponseCode() == 200) {
                    Platform.runLater(() -> {
                        hostnameField.clear();
                        interfaceIpField.clear();
                        loadingLabel.setText("✅ Device configured");
                        loadDevicesFromAPI();
                    });
                } else {
                    Platform.runLater(() -> loadingLabel.setText("❌ Failed to configure device"));
                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> loadingLabel.setText("⚠️ Error configuring device"));
            }
        }).start();
    }

    private static class LocalDateTimeAdapter implements JsonDeserializer<LocalDateTime>, com.google.gson.JsonSerializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return LocalDateTime.parse(json.getAsString());
        }

        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, com.google.gson.JsonSerializationContext context) {
            return new com.google.gson.JsonPrimitive(src.toString());
        }
    }
}