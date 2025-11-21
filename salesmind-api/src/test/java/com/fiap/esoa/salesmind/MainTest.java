
package com.fiap.esoa.salesmind;

import com.fiap.esoa.salesmind.util.JsonTestUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes da API REST SalesMind
 */
class MainTest {

    private static HttpServer server;
    private static HttpClient client;
    private static String accessToken;
    private static final String BASE_URL = "http://localhost:8080";
    private static final String TEST_EMAIL = "test.main." + System.currentTimeMillis() + "@test.com";
    private static final String TEST_PASSWORD = "senha123";

    @BeforeAll
    static void startServer() throws IOException {
        server = Main.startServer();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        setupAuth();
    }
    
    private static void setupAuth() {
        try {
            String uniqueCnpj = String.format("%014d", System.currentTimeMillis() % 100000000000000L);
            
            String signupRequest = "{" +
                "\"nomeEmpresa\": \"MainTest Company\"," +
                "\"cnpj\": \"" + uniqueCnpj + "\"," +
                "\"email\": \"" + TEST_EMAIL + "\"," +
                "\"senha\": \"" + TEST_PASSWORD + "\"" +
            "}";
            
            HttpRequest signup = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/signup"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(signupRequest))
                    .build();
            
            client.send(signup, HttpResponse.BodyHandlers.ofString());
            
            String loginRequest = "{" +
                "\"email\": \"" + TEST_EMAIL + "\"," +
                "\"senha\": \"" + TEST_PASSWORD + "\"" +
            "}";
            
            HttpRequest login = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(loginRequest))
                    .build();
            
            HttpResponse<String> loginResponse = client.send(login, HttpResponse.BodyHandlers.ofString());
            
            JsonNode loginJson = JsonTestUtil.parse(loginResponse.body());
            if (loginJson != null && loginJson.has("accessToken")) {
                accessToken = JsonTestUtil.getString(loginJson, "accessToken");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void testHealthEndpointGet() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/health"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        String body = response.body();
        assertTrue(body.contains("UP"), "Resposta deve conter 'UP'");
        assertTrue(body.contains("SalesMind API"), "Resposta deve conter 'SalesMind API'");

        assertTrue(response.headers().firstValue("Content-Type").orElse("")
                .contains("application/json"), "Content-Type deve ser application/json");
    }

    @Test
    void testEmpresaPost() throws IOException, InterruptedException {
        String uniqueCnpj = String.format("%014d", (System.currentTimeMillis() + 999) % 100000000000000L);
        String testData = "{\"nomeEmpresa\": \"Test Company\", \"cnpj\": \"" + uniqueCnpj + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/empresas"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(testData))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode(), "Response: " + response.body());

        String body = response.body();
        assertTrue(body.contains("Test Company"), "Resposta deve conter nome da empresa");
    }

    @Test
    void testHealthEndpointRejectsDelete() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/health"))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
    }
}
