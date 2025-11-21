package com.fiap.esoa.salesmind.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class BaseController implements HttpHandler {

    protected void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        addCorsHeaders(exchange);

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        sendResponse(exchange, statusCode, json);
    }

    protected void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        String errorJson = String.format("{\"error\":\"%s\",\"status\":%d}", message, statusCode);
        sendResponse(exchange, statusCode, errorJson);
    }

    protected String getRequestBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    protected String getPathParameter(HttpExchange exchange, int index) {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        return (parts.length > index) ? parts[index] : null;
    }

    protected void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
    
    protected Long getAuthenticatedEmpresaId(HttpExchange exchange) {
        return (Long) exchange.getAttribute("empresaId");
    }
    
    protected Long getAuthenticatedUserId(HttpExchange exchange) {
        return (Long) exchange.getAttribute("userId");
    }
    
    protected String getAuthenticatedRole(HttpExchange exchange) {
        return (String) exchange.getAttribute("role");
    }
    
    protected boolean isAdmin(HttpExchange exchange) {
        String role = getAuthenticatedRole(exchange);
        return "ADMIN".equals(role);
    }
    
    protected String getAuthenticatedEmail(HttpExchange exchange) {
        return (String) exchange.getAttribute("email");
    }
    
    protected boolean validateEmpresaAccess(HttpExchange exchange, Long resourceEmpresaId) {
        Long authenticatedEmpresaId = getAuthenticatedEmpresaId(exchange);
        return authenticatedEmpresaId != null && authenticatedEmpresaId.equals(resourceEmpresaId);
    }

    protected boolean handleCorsPreFlight(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (handleCorsPreFlight(exchange)) {
                return;
            }

            handleRequest(exchange);

        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        } finally {
            exchange.close();
        }
    }

    protected abstract void handleRequest(HttpExchange exchange) throws IOException;
}
