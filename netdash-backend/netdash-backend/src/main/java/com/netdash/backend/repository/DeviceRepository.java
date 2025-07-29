package com.netdash.backend.repository;

//package com.netdash.repository;

import com.netdash.backend.model.Device;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DeviceRepository extends MongoRepository<Device, String> {
    // You can add custom queries later
}
