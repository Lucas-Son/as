package com.fiap.esoa.salesmind.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utilitário para operações JSON em testes usando Jackson.
 */
public class JsonTestUtil {
    
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    public static JsonNode parse(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao fazer parse do JSON", e);
        }
    }
    
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao serializar para JSON", e);
        }
    }
    
    public static Long getLong(JsonNode node, String... path) {
        JsonNode current = node;
        for (String key : path) {
            current = current.get(key);
            if (current == null) {
                return null;
            }
        }
        return current.asLong();
    }
    
    public static String getString(JsonNode node, String... path) {
        JsonNode current = node;
        for (String key : path) {
            current = current.get(key);
            if (current == null) {
                return null;
            }
        }
        return current.asText();
    }
}
