package com.example.processing_service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.kafka.annotation.KafkaListener;
import com.example.common.model.FileMetadata;
import com.example.common.repository.FileMetadataRepository;
import java.nio.file.Path;

@Service
public class ProcessingService {

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Value("${file.processed-dir}")
    private String PROCESSED_DIR;

    private final FileMetadataRepository metadataRepository;

    public ProcessingService(FileMetadataRepository metadataRepository) {
        this.metadataRepository = metadataRepository;
    }

    @KafkaListener(topics = "file-upload-topic", groupId = "processing-group")
    public void processFile(String fileId, String fileContent) {
        try {
            // Determine file type and process accordingly
            if (fileId.endsWith(".pdf")) {
                extractTextFromPdf(fileId, fileContent);
            } else if (fileId.endsWith(".txt")) {
                // Handle text file processing
                processTextFile(fileId, fileContent);
            } else if (fileId.endsWith(".jpg") || fileId.endsWith(".png")) {
                // Handle image file processing
                processImageFile(fileId, fileContent);
            } else {
                compressFile(fileId, fileContent);
            }

        } catch (Exception e) {
            System.err.println("Error processing file: " + e.getMessage());
        }
    }

    public void processTextFile(String fileId, String fileContent) {
        // Logic to process text files
        System.out.println("Processing text file: " + fileId);
        // Add your text file processing logic here
    }

    public void processImageFile(String fileId, String fileContent) {
        // Logic to process image files
        System.out.println("Processing image file: " + fileId);
        // Add your image file processing logic here
    }

    public void extractTextFromPdf(String fileId, String fileContent) {

        try (PDDocument document = PDDocument.load(fileContent.getBytes())) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);
            
            // Save extracted text to file
            Path textPath = Paths.get(PROCESSED_DIR + fileId + ".txt");
            Files.write(textPath, text.getBytes());
            
            // Update metadata in MongoDB
            FileMetadata metadata = metadataRepository.findById(fileId).orElseThrow();
            metadata.setExtractedText(text);
            metadata.setExtractedTextLength(text.length());
            metadata.setStatus("Processed");
            metadataRepository.save(metadata);
            
        } catch (IOException e) {
            System.err.println("Error extracting text from PDF: " + e.getMessage());
        }
    }

    public void compressFile(String fileId, String fileContent) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Deflater deflater = new Deflater();
            deflater.setInput(fileContent.getBytes());
            deflater.finish();
            byte[] buffer = new byte[1024];
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            deflater.end();
            
            // Save compressed file
            Path compressedPath = Paths.get(PROCESSED_DIR + fileId + ".zip");
            Files.write(compressedPath, outputStream.toByteArray());
            
            // Calculate compression ratio
            double originalSize = fileContent.getBytes().length;
            double compressedSize = outputStream.size();
            double ratio = (originalSize - compressedSize) / originalSize * 100;
            
            // Update metadata in MongoDB
            FileMetadata metadata = metadataRepository.findById(fileId).orElseThrow();
            metadata.setCompressionRatio(ratio);
            metadataRepository.save(metadata);
            
        } catch (IOException e) {
            System.err.println("Error compressing file: " + e.getMessage());
        }
    }

    public void processFile(String fileId, String filePath, String operationType, String userId, String userIp) {
        executorService.submit(() -> {
            try {
                String basePath = Paths.get("").toAbsolutePath().toString(); // Get project root
                String processedFilePath = Paths.get(basePath, PROCESSED_DIR, fileId + "-processed").toString();

                Files.createDirectories(Paths.get(basePath, PROCESSED_DIR)); // Ensure directory exists
                
                // Simulating Processing
                System.out.println("Processing " + filePath + " with " + operationType + " for user: " + userId + " from IP: " + userIp);
                Files.copy(Paths.get(filePath), Paths.get(processedFilePath)); // Copy file after processing

                System.out.println("Processed file saved at: " + processedFilePath);
            } catch (Exception e) {
                System.err.println("Error processing file: " + e.getMessage());
            }
        });
    }
}
