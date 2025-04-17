package com.mpjmp.fileupload.kafka;

import com.mpjmp.fileupload.model.Device;
import com.mpjmp.fileupload.repository.DeviceRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.net.URL;
import java.util.Optional;

@Slf4j
@Service
public class ReplicationConsumer {
    @Value("${app.device.id}")
    private String thisDeviceId;
    @Value("${app.upload.dir}")
    private String uploadDir;
    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private ReplicationProducer replicationProducer;

    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MS = 5000;

    @KafkaListener(topics = "file-replication", groupId = "replication-group")
    public void listenReplication(ReplicationEvent event) {
        if (event.getTargetDeviceIds() == null || !event.getTargetDeviceIds().contains(thisDeviceId)) return;
        log.info("Replication event received for file {} to device {}", event.getFileName(), thisDeviceId);
        int attempt = 0;
        boolean success = false;
        Exception lastException = null;
        while (attempt < MAX_RETRIES && !success) {
            try {
                // Notify started
                ReplicationEvent started = new ReplicationEvent();
                started.setFileId(event.getFileId());
                started.setFileName(event.getFileName());
                started.setUploaderDeviceId(event.getUploaderDeviceId());
                started.setStatus("STARTED");
                started.setTargetDeviceId(thisDeviceId);
                replicationProducer.sendReplicationEvent("file-replication-status", started);

                // Download file
                String fileUrl = event.getDownloadUrl();
                File targetDir = new File(uploadDir, thisDeviceId);
                targetDir.mkdirs();
                File targetFile = new File(targetDir, event.getFileName());
                FileUtils.copyURLToFile(new URL(fileUrl), targetFile);
                log.info("File {} replicated to {}", event.getFileName(), targetFile.getAbsolutePath());

                // Notify done
                ReplicationEvent done = new ReplicationEvent();
                done.setFileId(event.getFileId());
                done.setFileName(event.getFileName());
                done.setUploaderDeviceId(event.getUploaderDeviceId());
                done.setStatus("DONE");
                done.setTargetDeviceId(thisDeviceId);
                replicationProducer.sendReplicationEvent("file-replication-status", done);
                success = true;
            } catch (Exception e) {
                lastException = e;
                log.error("Replication failed for file {} (attempt {}): {}", event.getFileName(), attempt + 1, e.getMessage());
                try { Thread.sleep(RETRY_DELAY_MS * (attempt + 1)); } catch (InterruptedException ignored) {}
                attempt++;
            }
        }
        if (!success) {
            ReplicationEvent failed = new ReplicationEvent();
            failed.setFileId(event.getFileId());
            failed.setFileName(event.getFileName());
            failed.setUploaderDeviceId(event.getUploaderDeviceId());
            failed.setStatus("FAILED");
            failed.setTargetDeviceId(thisDeviceId);
            failed.setMessage(lastException != null ? lastException.getMessage() : "Unknown error");
            replicationProducer.sendReplicationEvent("file-replication-status", failed);
        }
    }
}
