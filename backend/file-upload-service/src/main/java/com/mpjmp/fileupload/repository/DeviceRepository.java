package com.mpjmp.fileupload.repository;

import com.mpjmp.fileupload.model.Device;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DeviceRepository extends MongoRepository<Device, String> {
}
