package com.dataorchestrate.processing;

import com.dataorchestrate.common.DeviceIdentifier;
import com.dataorchestrate.processing.model.FileMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xssf.extractor.XSSFExcelExtractor;
import org.apache.poi.hssf.extractor.ExcelExtractor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(ProcessingService.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final DeviceIdentifier deviceIdentifier;
    private final String processingDir;
    private final MongoTemplate mongoTemplate;
    private final Tika tika;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public ProcessingService(
            KafkaTemplate<String, String> kafkaTemplate,
            DeviceIdentifier deviceIdentifier,
            MongoTemplate mongoTemplate,
            @Value("${file.processing.directory}") String processingDir,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.deviceIdentifier = deviceIdentifier;
        this.mongoTemplate = mongoTemplate;
        this.processingDir = processingDir;
        this.tika = new Tika();
        this.objectMapper = objectMapper;
        logger.info("Initializing ProcessingService for device: {}", deviceIdentifier.getDeviceId());
        createProcessingDirectory();
    }
    
    public void processFile(String fileId, String fileName, String uploadedBy) throws java.io.IOException {
        String deviceId = deviceIdentifier.getDeviceId();
        String deviceName = deviceIdentifier.getDeviceName();
        Path filePath = Paths.get(processingDir, deviceId, fileId + "_" + fileName);
        File file = filePath.toFile();
        
        if (!file.exists()) {
            logger.error("File not found at path: {}", filePath);
            throw new FileNotFoundException("File not found: " + filePath);
        }
        
        String fileType = tika.detect(file);
        FileMetadata metadata = new FileMetadata();
        metadata.setFileId(fileId);
        metadata.setFileName(fileName);
        metadata.setFileType(fileType);
        metadata.setFileSize(file.length());
        metadata.setDeviceId(deviceId);
        metadata.setDeviceName(deviceName);
        metadata.setUploadedBy(uploadedBy);
        metadata.setUploadTime(LocalDateTime.now());
        metadata.setStoragePath(filePath.toString());
        
        // Create data directory structure if it doesn't exist
        Path dataDir = Paths.get("data");
        Path deviceDataDir = dataDir.resolve(deviceId);
        if (!Files.exists(deviceDataDir)) {
            Files.createDirectories(deviceDataDir);
            logger.info("Created data directory for device: {}", deviceDataDir);
        }
        
        try {
            if (fileType.equals("application/pdf")) {
                processPdf(file, metadata);
            } else if (fileType.startsWith("image/")) {
                processImage(file, metadata);
            } else if (fileType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                processDocx(file, metadata);
            } else if (fileType.equals("application/vnd.ms-excel") || fileType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
                processExcel(file, metadata);
            } else {
                compressAndStore(file, metadata);
            }
            
            // Copy the processed file to the data directory
            Path targetPath = deviceDataDir.resolve(fileName);
            Files.copy(file.toPath(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            logger.info("Copied processed file to data directory: {}", targetPath);
            
            // Update metadata with the new storage path
            metadata.setStoragePath(targetPath.toString());
            metadata.setStatus("PROCESSED");
            metadata.setProcessedTime(LocalDateTime.now());
            
            // Save metadata to MongoDB
            mongoTemplate.save(metadata);
            logger.info("Saved metadata for processed file: {}", fileName);
            
            try {
                // Send notification about successful processing
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "SUCCESS");
                notification.put("message", "File processed successfully: " + fileName);
                notification.put("fileId", fileId);
                notification.put("deviceId", deviceId);
                notification.put("timestamp", LocalDateTime.now().toString());
                
                String notificationJson = objectMapper.writeValueAsString(notification);
                kafkaTemplate.send("notifications", notificationJson);
                logger.info("Sent processing success notification for file: {}", fileName);
            } catch (Exception e) {
                logger.error("Failed to send notification: {}", e.getMessage());
            }
        } catch (Exception e) {
            logger.error("Error processing file: {}", e.getMessage(), e);
            metadata.setStatus("ERROR");
            metadata.setErrorMessage(e.getMessage());
            mongoTemplate.save(metadata);
            
            try {
                // Send notification about processing failure
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "ERROR");
                notification.put("message", "File processing failed: " + fileName + " - " + e.getMessage());
                notification.put("fileId", fileId);
                notification.put("deviceId", deviceId);
                notification.put("timestamp", LocalDateTime.now().toString());
                
                String notificationJson = objectMapper.writeValueAsString(notification);
                kafkaTemplate.send("notifications", notificationJson);
                logger.info("Sent processing error notification for file: {}", fileName);
            } catch (Exception ex) {
                logger.error("Failed to send notification: {}", ex.getMessage());
            }
        }
    }
    
    private void processImage(File file, FileMetadata metadata) throws Exception {
        // Extract text from image using Tika
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();
        Metadata tikaMetadata = new Metadata();
        try (InputStream stream = new FileInputStream(file)) {
            parser.parse(stream, handler, tikaMetadata, null);
            metadata.setExtractedText(handler.toString());
        }
        
        // Store additional metadata
        Map<String, Object> additionalMetadata = new HashMap<>();
        additionalMetadata.put("width", tikaMetadata.get("Image Width"));
        additionalMetadata.put("height", tikaMetadata.get("Image Height"));
        metadata.setMetadata(additionalMetadata);
    }
    
    private void processPdf(File file, FileMetadata metadata) throws Exception {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(document.getNumberOfPages());
            
            String text = stripper.getText(document);
            metadata.setExtractedText(text);
            
            if (text != null && !text.isEmpty()) {
                logger.info("PDF extracted text (first 100 chars): {}", text.substring(0, Math.min(100, text.length())));
            } else {
                logger.warn("No text extracted from PDF: {}", file.getAbsolutePath());
                // Try alternative extraction method
                BodyContentHandler handler = new BodyContentHandler(-1); // -1 means no limit
                org.apache.tika.metadata.Metadata tikaMetadata = new org.apache.tika.metadata.Metadata();
                try (FileInputStream stream = new FileInputStream(file)) {
                    new AutoDetectParser().parse(stream, handler, tikaMetadata);
                    String tikaText = handler.toString();
                    if (tikaText != null && !tikaText.isEmpty()) {
                        metadata.setExtractedText(tikaText);
                        logger.info("Tika extracted text (first 100 chars): {}", 
                            tikaText.substring(0, Math.min(100, tikaText.length())));
                    }
                }
            }
            
            // Store additional metadata
            Map<String, Object> additionalMetadata = new HashMap<>();
            additionalMetadata.put("pageCount", document.getNumberOfPages());
            metadata.setMetadata(additionalMetadata);
            
            // Save extracted text to a separate file in the data directory
            Path dataDir = Paths.get("data", metadata.getDeviceId());
            Files.createDirectories(dataDir);
            
            Path textFilePath = dataDir.resolve(metadata.getFileName() + ".txt");
            Files.write(textFilePath, metadata.getExtractedText().getBytes());
            logger.info("Saved extracted text to: {}", textFilePath);
            
            // Persist metadata after extraction
            mongoTemplate.save(metadata);
            logger.info("Saved FileMetadata with extractedText for file: {}", file.getAbsolutePath());
        }
    }
    
    private void processDocx(File file, FileMetadata metadata) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new FileInputStream(file))) {
            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            metadata.setExtractedText(extractor.getText());
            
            // Store additional metadata
            Map<String, Object> additionalMetadata = new HashMap<>();
            additionalMetadata.put("paragraphCount", document.getParagraphs().size());
            metadata.setMetadata(additionalMetadata);
        }
    }
    
    private void processExcel(File file, FileMetadata metadata) throws Exception {
        String text;
        if (metadata.getFileType().equals("application/vnd.ms-excel")) {
            try (HSSFWorkbook workbook = new HSSFWorkbook(new FileInputStream(file))) {
                ExcelExtractor extractor = new ExcelExtractor(workbook);
                text = extractor.getText();
            }
        } else {
            try (XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(file))) {
                XSSFExcelExtractor extractor = new XSSFExcelExtractor(workbook);
                text = extractor.getText();
            }
        }
        metadata.setExtractedText(text);
    }
    
    private void compressAndStore(File file, FileMetadata metadata) throws Exception {
        Path compressedPath = Paths.get(processingDir, metadata.getDeviceId(), 
            metadata.getFileId() + "_" + metadata.getFileName() + ".zip");
        
        try (FileOutputStream fos = new FileOutputStream(compressedPath.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos);
             FileInputStream fis = new FileInputStream(file)) {
            
            ZipEntry zipEntry = new ZipEntry(metadata.getFileName());
            zos.putNextEntry(zipEntry);
            
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            
            zos.closeEntry();
        }
        
        metadata.setCompressed(true);
        metadata.setCompressionType("ZIP");
        metadata.setCompressedSize(compressedPath.toFile().length());
        metadata.setStoragePath(compressedPath.toString());
    }
    
    private void createProcessingDirectory() {
        File directory = new File(processingDir);
        if (!directory.exists()) {
            directory.mkdirs();
            logger.info("Created processing directory: {}", processingDir);
        }
    }
} 