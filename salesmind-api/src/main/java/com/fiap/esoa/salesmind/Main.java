
package com.fiap.esoa.salesmind;

import com.sun.net.httpserver.HttpServer;
import com.fiap.esoa.salesmind.config.EnvConfig;
import com.fiap.esoa.salesmind.controller.*;
import com.fiap.esoa.salesmind.filter.JwtAuthFilter;
import com.fiap.esoa.salesmind.repository.*;
import com.fiap.esoa.salesmind.service.*;
import com.fiap.esoa.salesmind.util.JwtKeyManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import javax.crypto.SecretKey;

/**
 * Main class for the SalesMind REST API
 * Uses Java's built-in HttpServer for lightweight REST endpoints
 */
public class Main {

    private static final int PORT = 8080;
    private static final String HOST = "localhost";
    
    static {
        EnvConfig.load();
    }

    public static HttpServer startServer() throws IOException {
        return startServer(PORT);
    }

    public static HttpServer startServer(int port) throws IOException {
        // Create HTTP server on specified port
        HttpServer server = HttpServer.create(new InetSocketAddress(HOST, port), 0);

        SecretKey jwtKey = JwtKeyManager.getSecretKey();

        EmpresaRepository empresaRepository = new EmpresaRepository();
        UsuarioRepository usuarioRepository = new UsuarioRepository();
        ClienteRepository clienteRepository = new ClienteRepository();
        GravacaoCallRepository gravacaoRepository = new GravacaoCallRepository();
        FeedbackIARepository feedbackRepository = new FeedbackIARepository();

        GeminiService geminiService = new GeminiService();
        EmpresaService empresaService = new EmpresaService(empresaRepository, usuarioRepository, clienteRepository, gravacaoRepository);
        UsuarioService usuarioService = new UsuarioService(usuarioRepository, gravacaoRepository);
        ClienteService clienteService = new ClienteService(clienteRepository, gravacaoRepository);
        FeedbackIAService feedbackService = new FeedbackIAService(feedbackRepository);
        GravacaoCallService gravacaoService = new GravacaoCallService(
                gravacaoRepository, usuarioRepository, feedbackService, geminiService);
        DashboardService dashboardService = new DashboardService(
                empresaRepository, gravacaoRepository);

        // Endpoints públicos
        server.createContext("/health", exchange -> {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }

            String response = "{\"status\":\"UP\",\"service\":\"SalesMind API\"}";
            byte[] bytes = response.getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        
        // Documentação Swagger UI
        server.createContext("/api/docs", exchange -> {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }
            
            String html = """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                    <meta charset="UTF-8">
                    <title>SalesMind API - Documentação</title>
                    <link rel="stylesheet" type="text/css" href="https://unpkg.com/swagger-ui-dist@5.10.3/swagger-ui.css">
                    <style>
                        body { margin: 0; padding: 0; }
                    </style>
                </head>
                <body>
                    <div id="swagger-ui"></div>
                    <script src="https://unpkg.com/swagger-ui-dist@5.10.3/swagger-ui-bundle.js"></script>
                    <script src="https://unpkg.com/swagger-ui-dist@5.10.3/swagger-ui-standalone-preset.js"></script>
                    <script>
                        window.onload = function() {
                            SwaggerUIBundle({
                                url: "/api/docs/openapi.yaml",
                                dom_id: '#swagger-ui',
                                deepLinking: true,
                                presets: [
                                    SwaggerUIBundle.presets.apis,
                                    SwaggerUIStandalonePreset
                                ],
                                plugins: [
                                    SwaggerUIBundle.plugins.DownloadUrl
                                ],
                                layout: "StandaloneLayout"
                            });
                        };
                    </script>
                </body>
                </html>
                """;
            
            byte[] bytes = html.getBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        
        // OpenAPI Spec - Arquivo YAML
        server.createContext("/api/docs/openapi.yaml", exchange -> {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }
            
            try (var inputStream = Main.class.getResourceAsStream("/openapi.yaml")) {
                if (inputStream == null) {
                    String error = "{\"error\":\"openapi.yaml not found\"}";
                    byte[] bytes = error.getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(404, bytes.length);
                    exchange.getResponseBody().write(bytes);
                    exchange.close();
                    return;
                }
                
                byte[] yamlContent = inputStream.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/yaml; charset=UTF-8");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, yamlContent.length);
                exchange.getResponseBody().write(yamlContent);
                exchange.close();
            }
        });
        
        // Endpoint de autenticação (login/refresh)
        server.createContext("/api/auth", new AuthController(jwtKey, usuarioService));
        
        // Endpoint de cadastro de empresa
        server.createContext("/api/signup", new SignupController(empresaService, usuarioService));

        // Endpoints protegidos por JWT
        server.createContext("/api/empresas", 
            new JwtAuthFilter(new EmpresaController(empresaService), jwtKey));
        server.createContext("/api/usuarios", 
            new JwtAuthFilter(new UsuarioController(usuarioService), jwtKey));
        server.createContext("/api/clientes", 
            new JwtAuthFilter(new ClienteController(clienteService), jwtKey));
        server.createContext("/api/gravacoes", 
            new JwtAuthFilter(new GravacaoCallController(gravacaoService, clienteService), jwtKey));
        server.createContext("/api/feedbacks", 
            new JwtAuthFilter(new FeedbackIAController(feedbackService, gravacaoService), jwtKey));
        server.createContext("/api/dashboard", 
            new JwtAuthFilter(new DashboardController(dashboardService), jwtKey));

        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        System.out.println("Iniciando servidor HTTP...");
        server.start();

        return server;
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = startServer();
        System.out.println(String.format(
                "API SalesMind iniciada em http://%s:%d/\n" +
                        "\nDOCUMENTAÇÃO SWAGGER: http://%s:%d/api/docs\n" +
                        "\nPressione enter para parar o servidor...",
                HOST, PORT, HOST, PORT, HOST, PORT, HOST, PORT, HOST, PORT,
                HOST, PORT, HOST, PORT, HOST, PORT, HOST, PORT, HOST, PORT,
                HOST, PORT, HOST, PORT, HOST, PORT, HOST, PORT));

        System.in.read();

        System.out.println("Parando servidor...");
        server.stop(0);
        System.out.println("Servidor parado.");
    }
}
