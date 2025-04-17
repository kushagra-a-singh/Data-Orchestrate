package com.mpjmp.fileupload.controller;

import com.mpjmp.fileupload.model.Device;
import com.mpjmp.fileupload.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    @Autowired
    private DeviceRepository deviceRepository;

    @PostMapping("/register")
    public Device registerDevice(@RequestBody Device device) {
        device.setRegisteredAt(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        device.setLastSeen(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        device.setStatus("ONLINE");
        return deviceRepository.save(device);
    }

    @GetMapping
    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }
}
