package com.fiap.esoa.salesmind.util;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MultipartParser {
    
    public static class FormData {
        private final Map<String, String> fields = new HashMap<>();
        private final Map<String, FileData> files = new HashMap<>();
        
        public String getField(String name) {
            return fields.get(name);
        }
        
        public FileData getFile(String name) {
            return files.get(name);
        }
        
        public void addField(String name, String value) {
            fields.put(name, value);
        }
        
        public void addFile(String name, FileData file) {
            files.put(name, file);
        }
    }
    
    public static class FileData {
        private final String filename;
        private final String contentType;
        private final byte[] data;
        
        public FileData(String filename, String contentType, byte[] data) {
            this.filename = filename;
            this.contentType = contentType;
            this.data = data;
        }
        
        public String getFilename() {
            return filename;
        }
        
        public String getContentType() {
            return contentType;
        }
        
        public byte[] getData() {
            return data;
        }
        
        public long getSize() {
            return data.length;
        }
    }
    
    public static FormData parse(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        
        if (contentType == null || !contentType.startsWith("multipart/form-data")) {
            throw new IllegalArgumentException("Content-Type must be multipart/form-data");
        }
        
        String boundary = extractBoundary(contentType);
        if (boundary == null) {
            throw new IllegalArgumentException("No boundary found in Content-Type");
        }
        
        InputStream inputStream = exchange.getRequestBody();
        byte[] bodyBytes = inputStream.readAllBytes();
        
        return parseMultipartData(bodyBytes, boundary);
    }
    
    private static String extractBoundary(String contentType) {
        for (String part : contentType.split(";")) {
            part = part.trim();
            if (part.startsWith("boundary=")) {
                return part.substring(9).trim();
            }
        }
        return null;
    }
    
    private static FormData parseMultipartData(byte[] data, String boundary) throws IOException {
        FormData formData = new FormData();
        String boundaryMarker = "--" + boundary;
        byte[] boundaryBytes = boundaryMarker.getBytes("UTF-8");
        
        int pos = 0;
        while (pos < data.length) {
            int boundaryStart = indexOf(data, boundaryBytes, pos);
            if (boundaryStart == -1) break;
            
            pos = boundaryStart + boundaryBytes.length;
            
            if (pos + 2 <= data.length && data[pos] == '\r' && data[pos + 1] == '\n') {
                pos += 2;
            } else if (pos + 1 <= data.length && data[pos] == '\n') {
                pos += 1;
            }
            
            if (pos + 2 <= data.length && data[pos] == '-' && data[pos + 1] == '-') {
                break;
            }
            
            int headerEnd = indexOf(data, "\r\n\r\n".getBytes(), pos);
            if (headerEnd == -1) {
                headerEnd = indexOf(data, "\n\n".getBytes(), pos);
                if (headerEnd == -1) break;
            }
            
            String headers = new String(data, pos, headerEnd - pos, "UTF-8");
            pos = headerEnd + (data[headerEnd] == '\r' ? 4 : 2);
            
            int nextBoundary = indexOf(data, boundaryBytes, pos);
            if (nextBoundary == -1) break;
            
            int contentEnd = nextBoundary - 2;
            if (contentEnd > pos && data[contentEnd - 1] == '\r') {
                contentEnd--;
            }
            
            byte[] content = new byte[contentEnd - pos];
            System.arraycopy(data, pos, content, 0, contentEnd - pos);
            
            String[] headerLines = headers.split("\\r?\\n");
            String fieldName = null;
            String filename = null;
            String contentType = "text/plain";
            
            for (String line : headerLines) {
                if (line.toLowerCase().startsWith("content-disposition:")) {
                    fieldName = extractValue(line, "name");
                    filename = extractValue(line, "filename");
                } else if (line.toLowerCase().startsWith("content-type:")) {
                    contentType = line.substring(13).trim();
                }
            }
            
            if (fieldName != null) {
                if (filename != null) {
                    formData.addFile(fieldName, new FileData(filename, contentType, content));
                } else {
                    formData.addField(fieldName, new String(content, "UTF-8"));
                }
            }
            
            pos = nextBoundary;
        }
        
        return formData;
    }
    
    private static String extractValue(String header, String name) {
        String search = name + "=\"";
        int start = header.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = header.indexOf("\"", start);
        if (end == -1) return null;
        return header.substring(start, end);
    }
    
    private static int indexOf(byte[] data, byte[] pattern, int start) {
        outer: for (int i = start; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
