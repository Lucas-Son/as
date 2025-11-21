package com.fiap.esoa.salesmind.config;

import io.github.cdimascio.dotenv.Dotenv;

public class EnvConfig {
    
    private static Dotenv dotenv;
    
    static {
        try {
            dotenv = Dotenv.configure()
                    .directory(".")
                    .ignoreIfMissing()
                    .load();
            
            dotenv.entries().forEach(entry -> {
                System.setProperty(entry.getKey(), entry.getValue());
            });
            
            System.out.println("Variáveis de ambiente carregadas do arquivo .env");
        } catch (Exception e) {
            System.out.println("Arquivo .env não encontrado, usando variáveis de ambiente do sistema");
        }
    }
    
    public static void load() {
        // Apenas para garantir que o bloco static seja executado
    }
    
    public static String get(String key) {
        return dotenv != null ? dotenv.get(key) : System.getenv(key);
    }
    
    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }
}
