package com.fiap.esoa.salesmind.e2e;

import com.fiap.esoa.salesmind.Main;
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
 * End-to-End tests for Feedback flow
 * Tests the complete flow: Empresa → Usuario → Cliente → GravacaoCall → FeedbackIA
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FeedbackE2ETest {

    private static HttpServer server;
    private static HttpClient httpClient;
    private static final String BASE_URL = "http://localhost:8080";

    private static Long empresaId;
    private static Long usuarioId;
    private static Long clienteId;
    private static Long gravacaoId;
    
    private static String accessToken;
    private static final String TEST_EMAIL = "admin.e2e." + System.currentTimeMillis() + "@test.com";
    private static final String TEST_PASSWORD = "senha123";
    
    // Unique identifiers for this test run
    private static final String UNIQUE_SUFFIX = String.valueOf(System.currentTimeMillis());

    @BeforeAll
    static void startServer() throws IOException {
        server = Main.startServer(8080);
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        setupAuthentication();
    }
    
    private static void setupAuthentication() throws IOException {
        try {
            String uniqueCnpj = String.format("%014d", System.currentTimeMillis() % 100000000000000L);
            
            String signupRequest = "{" +
                "\"nomeEmpresa\": \"E2E Test Company\"," +
                "\"cnpj\": \"" + uniqueCnpj + "\"," +
                "\"email\": \"" + TEST_EMAIL + "\"," +
                "\"senha\": \"" + TEST_PASSWORD + "\"" +
            "}";
            
            HttpRequest signup = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/signup"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(signupRequest))
                    .build();
            
            HttpResponse<String> signupResponse = httpClient.send(signup, HttpResponse.BodyHandlers.ofString());
            JsonNode signupJson = JsonTestUtil.parse(signupResponse.body());
            empresaId = JsonTestUtil.getLong(signupJson, "empresa", "id");
            usuarioId = JsonTestUtil.getLong(signupJson, "adminUser", "id");
            
            String loginRequest = "{" +
                "\"email\": \"" + TEST_EMAIL + "\"," +
                "\"senha\": \"" + TEST_PASSWORD + "\"" +
            "}";
            
            HttpRequest login = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(loginRequest))
                    .build();
            
            HttpResponse<String> loginResponse = httpClient.send(login, HttpResponse.BodyHandlers.ofString());
            JsonNode loginJson = JsonTestUtil.parse(loginResponse.body());
            accessToken = JsonTestUtil.getString(loginJson, "accessToken");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @Order(1)
    @DisplayName("E2E 1: Verificação de Saúde")
    void testHealthCheck() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/health"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("UP"));
    }

    @Test
    @Order(2)
    @DisplayName("E2E 2: Criar Cliente")
    void testCreateCliente() throws Exception {
        String uniqueCpf = String.format("%011d", Long.parseLong(UNIQUE_SUFFIX) % 100000000000L);

        String requestBody = String.format("""
                {
                    "nome": "Cliente E2E Test",
                    "cpfCnpj": "%s",
                    "telefone": "11999887766",
                    "segmento": "Tecnologia"
                }
                """, uniqueCpf);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/clientes"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        JsonNode json = JsonTestUtil.parse(response.body());
        clienteId = JsonTestUtil.getLong(json, "id");
        assertNotNull(clienteId);
    }

    @Test
    @Order(3)
    @DisplayName("E2E 3: Criar Gravação (sem processamento de áudio)")
    void testCreateGravacao() throws Exception {
        String requestBody = String.format("""
                {
                    "idCliente": %d
                }
                """, clienteId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/gravacoes"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode(), "Response: " + response.body());
        JsonNode json = JsonTestUtil.parse(response.body());
        gravacaoId = JsonTestUtil.getLong(json, "id");
        assertNotNull(gravacaoId);
    }

    @Test
    @Order(4)
    @DisplayName("E2E 4: Buscar Gravação com formato DTO")
    void testGetGravacaoWithDTO() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/gravacoes/" + gravacaoId))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        JsonNode json = JsonTestUtil.parse(response.body());
        
        assertEquals(gravacaoId, JsonTestUtil.getLong(json, "id"));
        assertEquals(usuarioId, JsonTestUtil.getLong(json, "idUsuario"));
        assertEquals(clienteId, JsonTestUtil.getLong(json, "idCliente"));
        assertTrue(json.has("statusProcessamento"));
        assertTrue(json.has("statusVenda"));
    }

    @Test
    @Order(5)
    @DisplayName("E2E 5: Listar todas as Gravações")
    void testListGravacoes() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/gravacoes"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        JsonNode array = JsonTestUtil.parse(response.body());
        
        assertTrue(array.size() > 0, "Deve ter pelo menos uma gravação");
    }

    @Test
    @Order(6)
    @DisplayName("E2E 6: Buscar métricas do Dashboard da empresa")
    void testDashboard() throws Exception {
        if (empresaId == null) {
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/dashboard/empresa/" + empresaId))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        JsonNode json = JsonTestUtil.parse(response.body());
        
        assertTrue(json.has("idEmpresa"));
        assertTrue(json.has("totalGravacoes"));
        assertTrue(json.has("totalClientes"));
        assertTrue(json.has("taxaConversao"));
        
        assertEquals(empresaId, JsonTestUtil.getLong(json, "idEmpresa"));
        assertTrue(JsonTestUtil.getLong(json, "totalGravacoes") >= 0);
        assertTrue(JsonTestUtil.getLong(json, "totalClientes") >= 0);
        assertTrue(json.get("taxaConversao").asDouble() >= 0.0 && json.get("taxaConversao").asDouble() <= 100.0);
    }

    @Test
    @Order(7)
    @DisplayName("E2E 7: Buscar Usuário com gravações")
    void testGetUsuarioWithGravacoes() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/usuarios/" + usuarioId))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        JsonNode json = JsonTestUtil.parse(response.body());
        
        assertEquals(usuarioId, JsonTestUtil.getLong(json, "id"));
        assertEquals(empresaId, JsonTestUtil.getLong(json, "idEmpresa"));
        assertTrue(json.has("funcao"), "Usuario DTO deve ter campo funcao");
    }

    @Test
    @Order(8)
    @DisplayName("E2E 8: 404 para recurso inexistente")
    void testNotFound() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/gravacoes/999999"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    @Order(9)
    @DisplayName("E2E 9: Corpo de requisição inválido retorna 400")
    void testBadRequest() throws Exception {
        System.out.println("\n[E2E 9] Testando 400 para requisição inválida");

        String invalidBody = """
                {
                    "nomeEmpresa": "",
                    "cnpj": "invalid"
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/empresas"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(invalidBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertTrue(response.statusCode() >= 400, "Deve retornar código de status de erro");
    }
}
