package com.fiap.esoa.salesmind.util;

import com.fiap.esoa.salesmind.exception.BusinessException;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class FileUploadUtil {
    
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("mp3", "wav", "m4a", "ogg", "flac", "mp4");
    private static final int FILE_RETENTION_DAYS = 30;
    
    private final String uploadBaseDir;
    
    public FileUploadUtil(String uploadBaseDir) {
        this.uploadBaseDir = uploadBaseDir;
        createUploadDirectory();
    }
    
    private void createUploadDirectory() {
        try {
            Files.createDirectories(Paths.get(uploadBaseDir));
        } catch (IOException e) {
            throw new BusinessException("Failed to create upload directory", e);
        }
    }
    
    public String saveFile(InputStream inputStream, String originalFilename, Long empresaId, Long clienteId) 
            throws IOException {
        
        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException("File type not allowed. Accepted: " + ALLOWED_EXTENSIONS);
        }
        
        String directoryPath = String.format("%s/%d/%d", uploadBaseDir, empresaId, clienteId);
        Files.createDirectories(Paths.get(directoryPath));
        
        String timestamp = LocalDateTime.now().toString().replace(":", "-");
        String filename = String.format("%s_%s.%s", 
                UUID.randomUUID().toString().substring(0, 8), 
                timestamp, 
                extension);
        String filePath = String.format("%s/%s", directoryPath, filename);
        
        Path targetPath = Paths.get(filePath);
        long bytesWritten = Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        if (bytesWritten > MAX_FILE_SIZE) {
            Files.deleteIfExists(targetPath);
            throw new BusinessException(
                String.format("File size exceeds maximum allowed size of %d MB", MAX_FILE_SIZE / 1024 / 1024));
        }
        
        return filePath;
    }
    
    public String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
    
    public void deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            System.err.println("Failed to delete file: " + filePath + " - " + e.getMessage());
        }
    }
    
    public CompletableFuture<Void> cleanupOldFiles() {
        return CompletableFuture.runAsync(() -> {
            try {
                LocalDateTime cutoffDate = LocalDateTime.now().minus(FILE_RETENTION_DAYS, ChronoUnit.DAYS);
                
                Files.walk(Paths.get(uploadBaseDir))
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        try {
                            LocalDateTime fileTime = LocalDateTime.ofInstant(
                                Files.getLastModifiedTime(path).toInstant(),
                                java.time.ZoneId.systemDefault());
                            return fileTime.isBefore(cutoffDate);
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            System.out.println("Deleted old file: " + path);
                        } catch (IOException e) {
                            System.err.println("Failed to delete old file: " + path + " - " + e.getMessage());
                        }
                    });
                
                System.out.println("Cleanup completed: removed files older than " + FILE_RETENTION_DAYS + " days");
                
            } catch (IOException e) {
                System.err.println("Error during file cleanup: " + e.getMessage());
            }
        });
    }
    
    public long getFileSize(String filePath) throws IOException {
        return Files.size(Paths.get(filePath));
    }
    
    public boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }
}
