package com.netdash.backend.service;

import com.netdash.backend.model.Device;
import com.netdash.backend.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NetconfService {
    private static final Logger logger = LoggerFactory.getLogger(NetconfService.class);

    @Autowired
    private DeviceRepository deviceRepository;

    public void configureDevice(Device device, Map<String, String> config) {
        if (device != null && config != null) {
            try {
                // Simulate NETCONF connection and config push
                String hostname = config.getOrDefault("hostname", device.getName());
                String interfaceIp = config.getOrDefault("interfaceIp", device.getIpAddress());

                device.setName(hostname);
                device.setIpAddress(interfaceIp);
                device.setProtocol("NETCONF");
                device.setStatus("Configured");
                deviceRepository.save(device);
                logger.info("NETCONF config applied to device: {}", device.getName());
            } catch (Exception e) {
                logger.error("Failed to configure device via NETCONF: {}", e.getMessage());
            }
        }
    }

    public void updateNetconfStatus(Device device) {
        if (device != null && "NETCONF".equals(device.getProtocol())) {
            // Simulate NETCONF status (e.g., CPU 50%, Memory 30% with variation)
            device.setCpuUsage(50.0 + Math.random() * 10);
            device.setMemoryUsage(30.0 + Math.random() * 10);
            deviceRepository.save(device);
            logger.debug("NETCONF status updated for device: {}", device.getName());
        }
    }
}