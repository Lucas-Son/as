package com.fiap.esoa.salesmind.util;

import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public class JwtKeyManager {
    private static final String ENV_KEY_NAME = "JWT_SECRET_KEY";
    private static final String KEY_FILE_PATH = "jwt-key.txt";
    
    private static SecretKey cachedKey;

    public static SecretKey getSecretKey() {
        if (cachedKey != null) {
            return cachedKey;
        }

        String envKey = System.getenv(ENV_KEY_NAME);
        if (envKey != null && !envKey.isEmpty()) {
            cachedKey = loadKeyFromBase64(envKey);
            return cachedKey;
        }

        File keyFile = new File(KEY_FILE_PATH);
        if (keyFile.exists()) {
            try {
                byte[] keyBytes = Files.readAllBytes(keyFile.toPath());
                cachedKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes);
                return cachedKey;
            } catch (IOException e) {
                System.err.println("Falha ao ler chave JWT: " + e.getMessage());
            }
        }
        
        try {
            SecretKey newKey = Jwts.SIG.HS512.key().build();
            Files.write(keyFile.toPath(), newKey.getEncoded());
            cachedKey = newKey;
            return cachedKey;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao gerar chave JWT: " + e.getMessage(), e);
        }
    }

    private static SecretKey loadKeyFromBase64(String base64Key) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            return io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Formato de chave JWT inválido", e);
        }
    }

    public static void clearCache() {
        cachedKey = null;
    }
}
