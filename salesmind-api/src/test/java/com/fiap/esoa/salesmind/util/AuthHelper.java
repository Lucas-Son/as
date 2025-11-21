package com.fiap.esoa.salesmind.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper para autenticação em testes E2E.
 */
public class AuthHelper {
    
    private final String baseUrl;
    private final HttpClient client;
    private final ObjectMapper objectMapper;
    private String accessToken;
    private String refreshToken;
    
    public AuthHelper(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Faz login com email e senha, armazena tokens para requisições subsequentes.
     */
    public boolean login(String email, String password) throws IOException, InterruptedException {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", email);
        credentials.put("senha", password);
        
        String requestBody = objectMapper.writeValueAsString(credentials);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
            this.accessToken = (String) responseBody.get("accessToken");
            this.refreshToken = (String) responseBody.get("refreshToken");
            return true;
        }
        
        return false;
    }
    
    /**
     * Retorna o token de acesso atual.
     */
    public String getAccessToken() {
        return accessToken;
    }
    
    /**
     * Retorna o token de refresh atual.
     */
    public String getRefreshToken() {
        return refreshToken;
    }
    
    /**
     * Cria um HttpRequest.Builder com header Authorization configurado.
     */
    public HttpRequest.Builder authenticatedRequest(String uri) {
        return HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Authorization", "Bearer " + accessToken);
    }
    
    /**
     * Cria um HttpRequest.Builder para requisições POST com header Authorization.
     */
    public HttpRequest.Builder authenticatedPost(String uri, String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
    }
    
    /**
     * Cria um HttpRequest.Builder para requisições PUT com header Authorization.
     */
    public HttpRequest.Builder authenticatedPut(String uri, String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body));
    }
    
    /**
     * Cria um HttpRequest.Builder para requisições DELETE com header Authorization.
     */
    public HttpRequest.Builder authenticatedDelete(String uri) {
        return HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Authorization", "Bearer " + accessToken)
                .DELETE();
    }
    
    /**
     * Atualiza o token de acesso usando o refresh token armazenado.
     */
    public boolean refreshAccessToken() throws IOException, InterruptedException {
        if (refreshToken == null) {
            return false;
        }
        
        Map<String, String> refreshRequest = new HashMap<>();
        refreshRequest.put("refreshToken", refreshToken);
        
        String requestBody = objectMapper.writeValueAsString(refreshRequest);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/auth/refresh"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
            this.accessToken = (String) responseBody.get("accessToken");
            return true;
        }
        
        return false;
    }
    
    /**
     * Limpa tokens armazenados (logout).
     */
    public void logout() {
        this.accessToken = null;
        this.refreshToken = null;
    }
    
    /**
     * Verifica se está logado (tem token de acesso).
     */
    public boolean isLoggedIn() {
        return accessToken != null;
    }
}
