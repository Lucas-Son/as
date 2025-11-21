package com.fiap.esoa.salesmind.filter;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.OutputStream;

public class JwtAuthFilter implements HttpHandler {
    
    private final HttpHandler delegate;
    private final SecretKey jwtKey;
    private final String[] publicPaths;

    public JwtAuthFilter(HttpHandler delegate, SecretKey jwtKey, String... publicPaths) {
        this.delegate = delegate;
        this.jwtKey = jwtKey;
        this.publicPaths = publicPaths != null ? publicPaths : new String[0];
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        
        String path = exchange.getRequestURI().getPath();
        
        if (isPublicPath(path)) {
            delegate.handle(exchange);
            return;
        }

        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(exchange, "Cabeçalho de autorização ausente ou inválido");
            return;
        }

        String token = authHeader.substring(7).trim();
        
        if (token.isEmpty() || token.split("\\.").length != 3) {
            sendUnauthorized(exchange, "Token JWT malformado");
            return;
        }

        try {
            Jws<Claims> jws = Jwts.parser()
                .verifyWith(jwtKey)
                .build()
                .parseSignedClaims(token);
            
            Claims claims = jws.getPayload();
            
            String tokenType = claims.get("type", String.class);
            if ("refresh".equals(tokenType)) {
                sendUnauthorized(exchange, "Não é possível usar token de atualização para acesso à API");
                return;
            }
            
            exchange.setAttribute("userId", claims.get("userId", Long.class));
            exchange.setAttribute("empresaId", claims.get("empresaId", Long.class));
            exchange.setAttribute("role", claims.get("role", String.class));
            exchange.setAttribute("email", claims.getSubject());
            
            delegate.handle(exchange);
            
        } catch (JwtException e) {
            System.err.println("Falha na validação do JWT: " + e.getMessage());
            sendUnauthorized(exchange, "Token inválido ou expirado");
        } catch (Exception e) {
            System.err.println("Erro inesperado durante autenticação: " + e.getMessage());
            e.printStackTrace();
            sendUnauthorized(exchange, "Erro de autenticação");
        }
    }

    private boolean isPublicPath(String path) {
        for (String publicPath : publicPaths) {
            if (path.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }
    
    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendUnauthorized(HttpExchange exchange, String message) throws IOException {
        String response = String.format("{\"error\":\"%s\"}", message);
        byte[] bytes = response.getBytes("UTF-8");
        
        addCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("WWW-Authenticate", "Bearer");
        exchange.sendResponseHeaders(401, bytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
