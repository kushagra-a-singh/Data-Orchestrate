package com.dataorchestrate.processing;

import com.dataorchestrate.common.DeviceIdentifier;
import com.dataorchestrate.processing.model.FileMetadata;
import org.springframework.beans.factory.annotation.Autowired;
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

@Service
public class ProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(ProcessingService.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final DeviceIdentifier deviceIdentifier;
    private final String processingDir;
    private final MongoTemplate mongoTemplate;
    private final Tika tika;
    
    @Autowired
    public ProcessingService(
            KafkaTemplate<String, String> kafkaTemplate,
            DeviceIdentifier deviceIdentifier,
            MongoTemplate mongoTemplate,
            @Value("${file.processing.directory}") String processingDir) {
        this.kafkaTemplate = kafkaTemplate;
        this.deviceIdentifier = deviceIdentifier;
        this.mongoTemplate = mongoTemplate;
        this.processingDir = processingDir;
        this.tika = new Tika();
        logger.info("Initializing ProcessingService for device: {}", deviceIdentifier.getDeviceId());
        createProcessingDirectory();
    }
    
    public void processFile(String fileId, String fileName, String uploadedBy) {
        String deviceId = deviceIdentifier.getDeviceId();
        logger.info("Processing file {} for device {}", fileName, deviceId);
        
        try {
            Path filePath = Paths.get(processingDir, deviceId, fileId + "_" + fileName);
            File file = filePath.toFile();
            
            // Detect file type
            String fileType = tika.detect(file);
            logger.info("Detected file type: {} for file: {}", fileType, fileName);
            
            // Create metadata
            FileMetadata metadata = new FileMetadata();
            metadata.setFileId(fileId);
            metadata.setFileName(fileName);
            metadata.setFileType(fileType);
            metadata.setFileSize(file.length());
            metadata.setDeviceId(deviceId);
            metadata.setUploadedBy(uploadedBy);
            metadata.setUploadTime(LocalDateTime.now());
            metadata.setStoragePath(filePath.toString());
            
            // Process based on file type
            if (fileType.startsWith("image/")) {
                processImage(file, metadata);
            } else if (fileType.equals("application/pdf")) {
                processPdf(file, metadata);
            } else if (fileType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                processDocx(file, metadata);
            } else if (fileType.equals("application/vnd.ms-excel") || 
                      fileType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
                processExcel(file, metadata);
            } else {
                // For other file types, just compress and store
                compressAndStore(file, metadata);
            }
            
            // Save metadata to MongoDB
            mongoTemplate.save(metadata);
            
            // Send notification
            kafkaTemplate.send("file.processed", 
                String.format("File %s processed by device %s", fileName, deviceId));
            
            logger.info("File {} processed successfully", fileName);
        } catch (Exception e) {
            logger.error("Error processing file {}: {}", fileName, e.getMessage());
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
            
            // Store additional metadata
            Map<String, Object> additionalMetadata = new HashMap<>();
            additionalMetadata.put("pageCount", document.getNumberOfPages());
            metadata.setMetadata(additionalMetadata);
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