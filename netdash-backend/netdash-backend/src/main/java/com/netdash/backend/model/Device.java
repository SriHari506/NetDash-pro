package com.netdash.backend.model;

import java.time.LocalDateTime;

public class Device {
    private String id;
    private String name;
    private String ipAddress;
    private String type;
    private String status;
    private double cpuUsage;
    private double memoryUsage;
    private LocalDateTime createdAt;
    private String macAddress;
    private String interfaceStatus;
    private String protocol;

    public Device() {}

    public Device(String name, String ipAddress, String status) {
        this.name = name;
        this.ipAddress = ipAddress;
        this.status = status;
    }

    public Device(String name, String ipAddress, String type, String status, double cpuUsage, double memoryUsage, String macAddress, String interfaceStatus, String protocol) {
        this.name = name;
        this.ipAddress = ipAddress;
        this.type = type;
        this.status = status;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.macAddress = macAddress;
        this.interfaceStatus = interfaceStatus;
        this.protocol = protocol;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }
    public double getMemoryUsage() { return memoryUsage; }
    public void setMemoryUsage(double memoryUsage) { this.memoryUsage = memoryUsage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
    public String getInterfaceStatus() { return interfaceStatus; }
    public void setInterfaceStatus(String interfaceStatus) { this.interfaceStatus = interfaceStatus; }
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
}