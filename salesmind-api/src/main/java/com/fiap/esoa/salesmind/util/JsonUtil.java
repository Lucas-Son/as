package com.fiap.esoa.salesmind.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class JsonUtil {
    
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(SerializationFeature.INDENT_OUTPUT);
    
    public static <T> String toJson(T object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao serializar objeto para JSON", e);
        }
    }
    
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao desserializar JSON para objeto", e);
        }
    }
    
    public static void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    public static void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        ErrorResponse error = new ErrorResponse(statusCode, message);
        sendJsonResponse(exchange, statusCode, error);
    }
    
    public static class ErrorResponse {
        private final int status;
        private final String error;
        private final String timestamp;
        
        public ErrorResponse(int status, String error) {
            this.status = status;
            this.error = error;
            this.timestamp = LocalDateTime.now().toString();
        }
        
        public int getStatus() { return status; }
        public String getError() { return error; }
        public String getTimestamp() { return timestamp; }
    }
}
