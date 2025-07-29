package com.netdash.backend.controller;

import com.netdash.backend.model.Device;
import com.netdash.backend.model.ApiResponse;
import com.netdash.backend.repository.DeviceRepository;
import com.netdash.backend.service.DeviceDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/devices")
@CrossOrigin(origins = "*")
public class DeviceController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceController.class);

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private DeviceDiscoveryService deviceDiscoveryService;

    // 🔹 GET all devices
    @GetMapping
    public ResponseEntity<ApiResponse<List<Device>>> getAllDevices() {
        logger.info("Fetching all devices...");
        List<Device> devices = deviceRepository.findAll();
        return ResponseEntity.ok(new ApiResponse<>(true, "Devices retrieved successfully", devices));
    }

    // 🔹 POST: Add a new device
    @PostMapping
    public ResponseEntity<ApiResponse<Device>> addDevice(@RequestBody Device device) {
        if (device.getName() == null || device.getIpAddress() == null) {
            logger.warn("Device creation failed: missing name or IP");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, "Device name and IP address are required", null));
        }

        device.setId(UUID.randomUUID().toString());
        device.setStatus("Online"); // Default status
        device.setCreatedAt(LocalDateTime.now());
        Device saved = deviceRepository.save(device);
        logger.info("Device added: {}", saved.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Device added successfully", saved));
    }

    // 🔹 PUT: Update a device
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Device>> updateDevice(@PathVariable String id, @RequestBody Device updated) {
        Optional<Device> existingDevice = deviceRepository.findById(id);

        if (existingDevice.isPresent()) {
            Device device = existingDevice.get();
            device.setName(updated.getName());
            device.setIpAddress(updated.getIpAddress());
            device.setStatus(updated.getStatus());
            device.setType(updated.getType());
            device.setCpuUsage(updated.getCpuUsage());
            device.setMemoryUsage(updated.getMemoryUsage());
            device.setMacAddress(updated.getMacAddress());
            device.setInterfaceStatus(updated.getInterfaceStatus());
            device.setProtocol(updated.getProtocol());

            Device saved = deviceRepository.save(device);
            logger.info("Device updated: {}", saved.getName());
            return ResponseEntity.ok(new ApiResponse<>(true, "Device updated successfully", saved));
        } else {
            logger.warn("Device not found for update: id={}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Device not found", null));
        }
    }

    // 🔹 DELETE: Delete device by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDevice(@PathVariable String id) {
        if (!deviceRepository.existsById(id)) {
            logger.warn("Attempt to delete non-existent device: id={}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Device not found", null));
        }

        deviceRepository.deleteById(id);
        logger.info("Device deleted: id={}", id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Device deleted successfully", null));
    }

    // 🔹 GET: Mock device stats (for graph polling)
    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Device>> getDeviceStatus(@PathVariable String id) {
        Optional<Device> deviceOpt = deviceRepository.findById(id);
        if (deviceOpt.isPresent()) {
            Device device = deviceOpt.get();
            // Simulate CPU and memory usage with historical tracking
            device.setCpuUsage(Math.random() * 100); // 0-100%
            device.setMemoryUsage(Math.random() * 100); // 0-100%
            logger.debug("Stats for device id={} => CPU: {}, Memory: {}", id, device.getCpuUsage(), device.getMemoryUsage());
            return ResponseEntity.ok(new ApiResponse<>(true, "Device status fetched", device));
        } else {
            logger.warn("Device not found for status: id={}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Device not found", null));
        }
    }

    // 🔹 GET: Discover new devices
    @GetMapping("/discover")
    public ResponseEntity<ApiResponse<List<Device>>> discoverDevices() {
        logger.info("Discovering new devices...");
        List<Device> devices = deviceDiscoveryService.discoverDevices();
        return ResponseEntity.ok(new ApiResponse<>(true, "Devices discovered", devices));
    }
}