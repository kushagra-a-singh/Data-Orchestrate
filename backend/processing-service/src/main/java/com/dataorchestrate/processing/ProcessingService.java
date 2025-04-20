package com.dataorchestrate.processing;

import com.dataorchestrate.common.DeviceIdentifier;
import com.dataorchestrate.common.NotificationSender;
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
        String deviceName = deviceIdentifier.getDeviceId(); // Or getName() if available
        Path filePath = Paths.get(processingDir, deviceId, fileId + "_" + fileName);
        File file = filePath.toFile();
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
        NotificationSender.sendNotification(
            "info", "Processing Started", "Processing file: " + fileName, 0.0, fileId, deviceId, filePath.toString()
        );
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
            NotificationSender.sendNotification(
                "success", "Processing Complete", "File processed and saved: " + filePath.toString(), 1.0, fileId, deviceId, filePath.toString()
            );
            metadata.setStatus("PROCESSED");
            try {
                kafkaTemplate.send("file.processed", objectMapper.writeValueAsString(metadata));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                logger.error("Failed to serialize metadata for processed file event", e);
            }
        } catch (Exception e) {
            logger.error("Error processing file {}: {}", fileName, e.getMessage());
            NotificationSender.sendNotification(
                "error", "Processing Failed", "Failed to process file: " + fileName + ". Error: " + e.getMessage(), null, fileId, deviceId, filePath.toString()
            );
            metadata.setStatus("FAILED");
            mongoTemplate.save(metadata);
            try {
                kafkaTemplate.send("file.processed", objectMapper.writeValueAsString(metadata));
            } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
                logger.error("Failed to serialize metadata for failed file event", ex);
            }
            throw new IOException(e);
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
            String text = stripper.getText(document);
            metadata.setExtractedText(text);
            if (text != null && !text.isEmpty()) {
                logger.info("PDF extracted text (first 100 chars): {}", text.substring(0, Math.min(100, text.length())));
            } else {
                logger.warn("No text extracted from PDF: {}", file.getAbsolutePath());
            }
            // Store additional metadata
            Map<String, Object> additionalMetadata = new HashMap<>();
            additionalMetadata.put("pageCount", document.getNumberOfPages());
            metadata.setMetadata(additionalMetadata);
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