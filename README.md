# NetDash Pro

A JavaFX and Spring Boot-based desktop dashboard for monitoring and managing networking devices using RESTful APIs and network protocols (SNMP, NETCONF, with plans for RESTCONF and GNMI). The project uses MongoDB as the persistent database.

## Project Overview

- **Goal**: Build a comprehensive network management tool with real-time monitoring and configuration capabilities.
- **Technologies**:
  - Backend: Spring Boot, MongoDB, SNMP4J
  - Frontend: JavaFX
  - Protocols: SNMP (implemented), NETCONF (simulated), RESTCONF & GNMI (planned)
- **Status**: In development, currently up to Day 4 of a 7-day plan (Phase 1, 2, and partial Phase 3).

## Features

### Current Features (Up to Day 4)
- **Phase 1: Project Skeleton & Setup**
  - Initialized Spring Boot backend with REST APIs (GET `/api/devices`, POST `/api/devices`).
  - Set up JavaFX frontend with a dashboard displaying devices in a TableView.
- **Phase 2: Core Features Implementation**
  - CRUD operations for devices (Add, Edit, Delete) with MongoDB persistence.
  - Live monitoring with simulated CPU and memory stats, displayed in a JavaFX LineChart.
- **Phase 3: Protocol Integration (Partial)**
  - SNMP integration using SNMP4J for real-time device metrics (CPU usage, interface status).
  - Simulated NETCONF support for configuration (e.g., hostname, interface IPs).

### Planned Features
- **Phase 3 (Remaining)**: Full NETCONF/YANG integration, optional RESTCONF, and GNMI support.
- **Phase 4**: UI/UX enhancements (Material Design, dark/light mode, notifications).
- **Phase 5**: Testing, Docker integration, and packaging into an executable installer.

## Project Structure

- `netdash-backend/`: Spring Boot application with REST APIs and protocol handlers.
- `netdash-client/`: JavaFX frontend for the dashboard UI.

## Setup Instructions

### Prerequisites
- Java 17 or higher
- Maven
- Git
- MongoDB (local instance or embedded)
- IDE (e.g., Eclipse)

### Installation

1. **Clone the Repository**
   ```bash
   git clone https://github.com/SriHari506/NetDash-pro.git
   cd NetDash-pro
