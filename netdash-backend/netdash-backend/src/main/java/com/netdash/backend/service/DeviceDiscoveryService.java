package com.netdash.backend.service;

import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.software.os.windows.WindowsOperatingSystem;
import oshi.util.ExecutingCommand;
import com.netdash.backend.model.Device;
import com.netdash.backend.repository.DeviceRepository;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeviceDiscoveryService {
    private static final Logger logger = LoggerFactory.getLogger(DeviceDiscoveryService.class);

    @Autowired
    private DeviceRepository deviceRepository;

    public List<Device> discoverDevices() {
        List<Device> devices = new ArrayList<>();
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        OperatingSystem os = si.getOperatingSystem();

        // Remove existing router entries to avoid duplicates
        deviceRepository.deleteById("Local Router");

        // Detect USB devices
        hal.getUsbDevices(true).forEach(usb -> {
            Device device = new Device(usb.getName(), "N/A", "USB", "Connected", 0.0, 0.0, null, null, null);
            devices.add(device);
            deviceRepository.save(device);
        });

        // Dynamically detect router IP
        String routerIp = getRouterIp(os);
        if (routerIp != null) {
            Device router = new Device("Local Router", routerIp, "Router", "Online", 0.0, 0.0, null, null, "SNMP");
            devices.add(router);
            deviceRepository.save(router);
        }

        // Discover additional network devices via ARP table
        discoverNetworkDevices(devices);

        return devices;
    }

    private String getRouterIp(OperatingSystem os) {
        if (os instanceof WindowsOperatingSystem) {
            try {
                List<String> output = ExecutingCommand.runNative("ipconfig");
                logger.debug("ipconfig output: {}", output);
                for (String line : output) {
                    logger.debug("Checking line: {}", line);
                    if (line.contains("Default Gateway")) {
                        String[] parts = line.split(":");
                        if (parts.length > 1) {
                            String ip = parts[1].trim();
                            if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                logger.info("Detected router IP: {}", ip);
                                return ip;
                            }
                        }
                    }
                }
                logger.warn("No valid Default Gateway found in ipconfig output");
            } catch (Exception e) {
                logger.error("Failed to get router IP on Windows", e);
            }
        } else {
            logger.warn("Non-Windows OS detected, router IP detection not fully implemented");
            return null;
        }
        return null;
    }

    private void discoverNetworkDevices(List<Device> devices) {
        try {
            List<String> arpOutput = ExecutingCommand.runNative("arp -a");
            for (String line : arpOutput) {
                if (line.contains("dynamic") && line.contains(".")) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length > 1) {
                        String ip = parts[0];
                        String mac = parts[1];
                        Device device = new Device("Network Device - " + ip, ip, "Network", "Online", 0.0, 0.0, mac, null, "SNMP");
                        devices.add(device);
                        deviceRepository.save(device);
                        updateDeviceMetrics(device); // Fetch initial SNMP metrics
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to discover network devices via ARP", e);
        }
    }

    public void updateDeviceMetrics(Device device) {
        try {
            TransportMapping transport = new DefaultUdpTransportMapping();
            Snmp snmp = new Snmp(transport);
            transport.listen();

            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString("public"));
            target.setAddress(new UdpAddress(device.getIpAddress() + "/161"));
            target.setRetries(2);
            target.setTimeout(1000);
            target.setVersion(SnmpConstants.version2c);

            PDU pdu = new PDU();
            pdu.add(new org.snmp4j.smi.VariableBinding(new OID(".1.3.6.1.2.1.25.3.3.1.2.1"))); // hrProcessorLoad (CPU)
            pdu.add(new org.snmp4j.smi.VariableBinding(new OID(".1.3.6.1.2.1.25.2.3.1.6.1"))); // hrStorageUsed (Memory)
            pdu.setType(PDU.GET);

            ResponseEvent response = snmp.send(pdu, target);
            if (response != null && response.getResponse() != null) {
                @SuppressWarnings("unchecked")
                List<org.snmp4j.smi.VariableBinding> bindings = (List<org.snmp4j.smi.VariableBinding>) (List<?>) response.getResponse().getVariableBindings();
                if (bindings.size() >= 2) {
                    double cpuUsage = Double.parseDouble(bindings.get(0).getVariable().toString());
                    double memoryUsage = Double.parseDouble(bindings.get(1).getVariable().toString()) / 1024; // Approx in MB
                    device.setCpuUsage(cpuUsage);
                    device.setMemoryUsage(memoryUsage);
                    deviceRepository.save(device);
                }
            }
            snmp.close();
        } catch (IOException e) {
            logger.warn("SNMP metrics fetch failed for IP {}: {}", device.getIpAddress(), e.getMessage());
        }
    }

    private String getSnmpInterfaceStatus(String ip) {
        try {
            TransportMapping transport = new DefaultUdpTransportMapping();
            Snmp snmp = new Snmp(transport);
            transport.listen();

            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString("public"));
            target.setAddress(new UdpAddress(ip + "/161"));
            target.setRetries(2);
            target.setTimeout(1000);
            target.setVersion(SnmpConstants.version2c);

            PDU pdu = new PDU();
            pdu.add(new org.snmp4j.smi.VariableBinding(new OID(".1.3.6.1.2.1.2.2.1.8.1"))); // ifOperStatus
            pdu.setType(PDU.GET);

            ResponseEvent response = snmp.send(pdu, target);
            if (response != null && response.getResponse() != null) {
                String status = response.getResponse().getVariableBindings().get(0).getVariable().toString();
                snmp.close();
                return status.equals("1") ? "Up" : "Down";
            }
            snmp.close();
        } catch (IOException e) {
            logger.warn("SNMP query failed for IP {}: {}", ip, e.getMessage());
        }
        return null;
    }
}