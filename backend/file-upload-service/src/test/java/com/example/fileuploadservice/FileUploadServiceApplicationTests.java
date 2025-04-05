package com.example.fileuploadservice;

import com.example.common.model.FileMetadata;
import com.example.common.repository.FileMetadataRepository;
import com.example.fileuploadservice.service.FileStorageServiceImpl;
import com.example.fileuploadservice.config.FileUploadProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceApplicationTests {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private KafkaTemplate<String, FileMetadata> kafkaTemplate;

    @Mock
    private FileUploadProperties properties;

    @InjectMocks
    private FileStorageServiceImpl fileStorageService;

    private Path uploadDir;

    @BeforeEach
    void setUp() throws IOException {
        uploadDir = Path.of("./test-uploads");
        if (Files.exists(uploadDir)) {
            Files.walk(uploadDir)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        }
        Files.createDirectories(uploadDir);
    }

    @Test
    void testStoreFileAsync() throws IOException {
        // Set up properties mock
        when(properties.getDir()).thenReturn("./test-uploads");

        // Create a test file
        File testFile = new File(uploadDir.toFile(), "test.txt");
        testFile.createNewFile();

        // Create test metadata map
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put("fileName", "test.txt");
        metadataMap.put("fileType", "text/plain");

        // Create and configure MultipartFile mock
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
        when(multipartFile.getContentType()).thenReturn("text/plain");
        when(multipartFile.getSize()).thenReturn(100L);
        when(multipartFile.getInputStream()).thenReturn(Files.newInputStream(testFile.toPath()));

        // Mock repository behavior to return the saved metadata
        when(fileMetadataRepository.save(any(FileMetadata.class))).thenAnswer(invocation -> {
            FileMetadata metadata = invocation.getArgument(0);
            return metadata;
        });

        // Test file storage
        CompletableFuture<FileMetadata> future = fileStorageService.storeFileAsync(multipartFile, metadataMap);
        FileMetadata result = future.join();

        // Verify
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("test.txt", result.getFileName());
        assertEquals("text/plain", result.getFileType());
        assertEquals(100L, result.getFileSize());
        assertEquals("PENDING", result.getStatus());
        
        verify(fileMetadataRepository).save(any(FileMetadata.class));
        verify(kafkaTemplate).send(eq("file-upload"), eq(result.getId()), eq(result));
    }

    @Test
    void testGetFileMetadata() {
        // Create test metadata
        String fileId = UUID.randomUUID().toString();
        FileMetadata metadata = FileMetadata.builder()
            .id(fileId)
            .fileName("test.txt")
            .fileSize(100L)
            .fileType("text/plain")
            .status(FileMetadata.Status.PENDING)
            .storagePath("./test-uploads/test.txt")
            .build();

        // Mock repository behavior
        when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(metadata));

        // Test getting file metadata
        Optional<FileMetadata> result = fileStorageService.getFileMetadata(fileId);

        // Verify
        assertTrue(result.isPresent());
        assertEquals(fileId, result.get().getId());
        assertEquals("test.txt", result.get().getFileName());
        verify(fileMetadataRepository).findById(fileId);
    }

    @Test
    void testGetFileMetadataNotFound() {
        // Mock repository behavior
        String fileId = UUID.randomUUID().toString();
        when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.empty());

        // Test getting file metadata
        Optional<FileMetadata> result = fileStorageService.getFileMetadata(fileId);
        assertTrue(result.isEmpty());
        verify(fileMetadataRepository).findById(fileId);
    }

    @Test
    void testListFiles() {
        // Create test metadata list
        String fileId = UUID.randomUUID().toString();
        FileMetadata metadata = FileMetadata.builder()
            .id(fileId)
            .fileName("test.txt")
            .fileSize(100L)
            .fileType("text/plain")
            .status(FileMetadata.Status.PROCESSING)
            .storagePath("./test-uploads/test.txt")
            .build();

        // Mock repository behavior
        when(fileMetadataRepository.findAll()).thenReturn(Arrays.asList(metadata));

        // Test listing files
        List<FileMetadata> results = fileStorageService.listFiles(FileMetadata.Status.PROCESSING);

        // Verify
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(fileId, results.get(0).getId());
        assertEquals("test.txt", results.get(0).getFileName());
        verify(fileMetadataRepository).findAll();
    }
}
