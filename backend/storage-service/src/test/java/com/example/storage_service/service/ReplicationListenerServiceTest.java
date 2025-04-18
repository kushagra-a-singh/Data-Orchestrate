package com.example.storage_service.service;

import com.example.storage_service.model.ReplicationRequest;
import com.example.storage_service.repository.DeviceRepository;
import com.example.storage_service.service.StorageService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReplicationListenerServiceTest {
    @Mock
    private StorageService storageService;
    @Mock
    private DeviceRepository deviceRepository;
    @InjectMocks
    private ReplicationListenerService replicationListenerService;

    public ReplicationListenerServiceTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testReceiveFileReplicationRequest() {
        ReplicationRequest request = new ReplicationRequest();
        request.setFileId("file123");
        when(deviceRepository.findAll()).thenReturn(Collections.emptyList());
        ResponseEntity<String> response = replicationListenerService.receiveFileReplicationRequest(request);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("File replicated successfully", response.getBody());
    }
}
