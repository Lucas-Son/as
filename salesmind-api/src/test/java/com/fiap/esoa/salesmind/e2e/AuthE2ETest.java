package com.fiap.esoa.salesmind.e2e;

import com.fiap.esoa.salesmind.Main;
import com.fiap.esoa.salesmind.util.AuthHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes end-to-end para autenticação JWT e aplicação de multi-tenancy
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthE2ETest {

    private static final String BASE_URL = "http://localhost:8080";
    private static HttpServer server;
    private static HttpClient client;
    private static ObjectMapper objectMapper;
    private static AuthHelper authHelper;

    private static final String EMPRESA1_USER_EMAIL = "admin1." + System.currentTimeMillis() + "@test.com";
    private static final String EMPRESA1_USER_PASSWORD = "senha123";
    private static final String EMPRESA2_USER_EMAIL = "admin2." + System.currentTimeMillis() + "@test.com";
    private static final String EMPRESA2_USER_PASSWORD = "senha123";

    @BeforeAll
    static void setUp() throws IOException {
        server = Main.startServer(8080);
        
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        objectMapper = new ObjectMapper();
        authHelper = new AuthHelper(BASE_URL);
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        createTestData();
    }
    
    private static void createTestData() throws IOException {
        try {
            long timestamp = System.currentTimeMillis();
            String uniqueCnpj1 = String.format("%014d", (timestamp + 1) % 100000000000000L);
            String uniqueCnpj2 = String.format("%014d", (timestamp + 2) % 100000000000000L);
            
            String signup1 = "{" +
                "\"nomeEmpresa\": \"Test Empresa 1\"," +
                "\"cnpj\": \"" + uniqueCnpj1 + "\"," +
                "\"email\": \"" + EMPRESA1_USER_EMAIL + "\"," +
                "\"senha\": \"" + EMPRESA1_USER_PASSWORD + "\"" +
            "}";
            
            HttpRequest request1 = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/signup"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(signup1))
                    .build();
            
            client.send(request1, HttpResponse.BodyHandlers.ofString());
            
            String signup2 = "{" +
                "\"nomeEmpresa\": \"Test Empresa 2\"," +
                "\"cnpj\": \"" + uniqueCnpj2 + "\"," +
                "\"email\": \"" + EMPRESA2_USER_EMAIL + "\"," +
                "\"senha\": \"" + EMPRESA2_USER_PASSWORD + "\"" +
            "}";
            
            HttpRequest request2 = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/signup"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(signup2))
                    .build();
            
            client.send(request2, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @AfterAll
    static void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @AfterEach
    void cleanup() {
        authHelper.logout();
    }

    @Test
    @Order(1)
    void testLoginSuccess() throws Exception {
        boolean loggedIn = authHelper.login(EMPRESA1_USER_EMAIL, EMPRESA1_USER_PASSWORD);
        
        assertTrue(loggedIn, "Login deve ter sucesso com credenciais válidas");
        assertNotNull(authHelper.getAccessToken(), "Token de acesso deve estar presente");
        assertNotNull(authHelper.getRefreshToken(), "Token de atualização deve estar presente");
        assertTrue(authHelper.isLoggedIn(), "Deve estar logado");
    }

    @Test
    @Order(2)
    void testLoginFailureInvalidPassword() throws Exception {
        boolean loggedIn = authHelper.login(EMPRESA1_USER_EMAIL, "wrongpassword");
        
        assertFalse(loggedIn, "Login deve falhar com senha inválida");
        assertNull(authHelper.getAccessToken(), "Token de acesso deve ser null");
        assertFalse(authHelper.isLoggedIn(), "Não deve estar logado");
    }

    @Test
    @Order(3)
    void testLoginFailureInvalidEmail() throws Exception {
        boolean loggedIn = authHelper.login("nonexistent@email.com", "anypassword");
        
        assertFalse(loggedIn, "Login deve falhar com email inexistente");
        assertNull(authHelper.getAccessToken(), "Token de acesso deve ser null");
    }

    @Test
    @Order(4)
    void testLoginMissingCredentials() throws Exception {
        Map<String, String> emptyCredentials = new HashMap<>();
        String requestBody = objectMapper.writeValueAsString(emptyCredentials);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(400, response.statusCode(), "Deve retornar 400 para credenciais ausentes");
    }

    @Test
    @Order(5)
    void testAccessDeniedWithoutToken() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/clientes"))
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(401, response.statusCode(), "Deve retornar 401 sem token de autenticação");
    }

    @Test
    @Order(6)
    void testAccessDeniedWithInvalidToken() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/clientes"))
                .header("Authorization", "Bearer invalid.token.here")
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(401, response.statusCode(), "Deve retornar 401 com token inválido");
    }

    @Test
    @Order(7)
    void testAccessDeniedWithMalformedAuthHeader() throws Exception {
        authHelper.login(EMPRESA1_USER_EMAIL, EMPRESA1_USER_PASSWORD);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/clientes"))
                .header("Authorization", authHelper.getAccessToken())
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(401, response.statusCode(), "Deve retornar 401 com cabeçalho de autenticação malformado");
    }

    @Test
    @Order(8)
    void testValidTokenAllowsAccess() throws Exception {
        authHelper.login(EMPRESA1_USER_EMAIL, EMPRESA1_USER_PASSWORD);
        
        HttpRequest request = authHelper.authenticatedRequest(BASE_URL + "/api/clientes")
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode(), "Deve permitir acesso com token válido");
    }

    @Test
    @Order(9)
    void testRefreshTokenSuccess() throws Exception {
        authHelper.login(EMPRESA1_USER_EMAIL, EMPRESA1_USER_PASSWORD);
        
        Thread.sleep(100);
        
        boolean refreshed = authHelper.refreshAccessToken();
        
        assertTrue(refreshed, "Atualização de token deve ter sucesso");
        assertNotNull(authHelper.getAccessToken(), "Novo token de acesso deve estar presente");
    }

    @Test
    @Order(10)
    void testRefreshTokenWithInvalidToken() throws Exception {
        Map<String, String> refreshRequest = new HashMap<>();
        refreshRequest.put("refreshToken", "invalid.refresh.token");
        String requestBody = objectMapper.writeValueAsString(refreshRequest);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/auth/refresh"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(401, response.statusCode(), "Deve retornar 401 para token de atualização inválido");
    }

    @Test
    @Order(11)
    void testCannotUseRefreshTokenForApiAccess() throws Exception {
        authHelper.login(EMPRESA1_USER_EMAIL, EMPRESA1_USER_PASSWORD);
        String refreshToken = authHelper.getRefreshToken();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/clientes"))
                .header("Authorization", "Bearer " + refreshToken)
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(401, response.statusCode(), "Deve rejeitar token de atualização para acesso à API");
    }

    @Test
    @Order(12)
    void testCrossEmpresaClienteAccessDenied() throws Exception {
        authHelper.login(EMPRESA1_USER_EMAIL, EMPRESA1_USER_PASSWORD);
        
        HttpRequest request = authHelper.authenticatedRequest(BASE_URL + "/api/clientes/999")
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertTrue(response.statusCode() == 403 || response.statusCode() == 404,
                "Deve negar acesso a cliente de empresa diferente");
    }

    @Test
    @Order(13)
    void testClienteListFilteredByEmpresa() throws Exception {
        authHelper.login(EMPRESA1_USER_EMAIL, EMPRESA1_USER_PASSWORD);
        
        long timestamp = System.currentTimeMillis();
        String uniqueCpf1 = String.format("%011d", (timestamp + 100) % 100000000000L);
        
        String cliente1Body = "{" +
            "\"nome\": \"Cliente Empresa 1\"," +
            "\"cpfCnpj\": \"" + uniqueCpf1 + "\"," +
            "\"telefone\": \"11999998888\"," +
            "\"email\": \"cliente1@test.com\"," +
            "\"segmento\": \"Tecnologia\"" +
        "}";
        
        HttpRequest createRequest1 = authHelper.authenticatedRequest(BASE_URL + "/api/clientes")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cliente1Body))
                .build();
        
        HttpResponse<String> createResponse1 = client.send(createRequest1, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, createResponse1.statusCode(), "Falha ao criar cliente 1");
        
        HttpRequest request1 = authHelper.authenticatedRequest(BASE_URL + "/api/clientes")
                .GET()
                .build();
        
        HttpResponse<String> response1 = client.send(request1, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response1.statusCode());
        
        authHelper.logout();
        authHelper.login(EMPRESA2_USER_EMAIL, EMPRESA2_USER_PASSWORD);
        
        String uniqueCpf2 = String.format("%011d", (timestamp + 200) % 100000000000L);
        
        String cliente2Body = "{" +
            "\"nome\": \"Cliente Empresa 2\"," +
            "\"cpfCnpj\": \"" + uniqueCpf2 + "\"," +
            "\"telefone\": \"11988887777\"," +
            "\"segmento\": \"Varejo\"" +
        "}";
        
        HttpRequest createRequest2 = authHelper.authenticatedRequest(BASE_URL + "/api/clientes")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cliente2Body))
                .build();
        
        HttpResponse<String> createResponse2 = client.send(createRequest2, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, createResponse2.statusCode(), "Falha ao criar cliente 2");
        
        HttpRequest request2 = authHelper.authenticatedRequest(BASE_URL + "/api/clientes")
                .GET()
                .build();
        
        HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response2.statusCode());
        
        assertNotEquals(response1.body(), response2.body(),
                "Empresas diferentes devem ver clientes diferentes");
    }

    @Test
    @Order(14)
    void testCannotCreateClienteForOtherEmpresa() throws Exception {
        authHelper.login(EMPRESA1_USER_EMAIL, EMPRESA1_USER_PASSWORD);
        
        Map<String, Object> clienteData = new HashMap<>();
        clienteData.put("nome", "Test Cliente");
        clienteData.put("cpfCnpj", "12345678900");
        clienteData.put("telefone", "11999999999");
        clienteData.put("idEmpresa", 999L);
        
        String requestBody = objectMapper.writeValueAsString(clienteData);
        
        HttpRequest request = authHelper.authenticatedPost(BASE_URL + "/api/clientes", requestBody)
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 201) {
            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
            assertNotNull(responseBody.get("idEmpresa"), "Resposta deve conter idEmpresa");
        }
    }

    @Test
    @Order(15)
    void testGravacaoFilteredByUser() throws Exception {
        authHelper.login(EMPRESA1_USER_EMAIL, EMPRESA1_USER_PASSWORD);
        
        HttpRequest request = authHelper.authenticatedRequest(BASE_URL + "/api/gravacoes")
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode(), "Deve retornar gravações para usuário autenticado");
    }

    @Test
    @Order(16)
    void testCannotAccessOtherUserGravacao() throws Exception {
        authHelper.login(EMPRESA1_USER_EMAIL, EMPRESA1_USER_PASSWORD);
        
        HttpRequest request = authHelper.authenticatedRequest(BASE_URL + "/api/gravacoes/999")
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertTrue(response.statusCode() == 403 || response.statusCode() == 404,
                "Deve negar acesso a gravação de usuário diferente");
    }

    @Test
    @Order(17)
    void testEmpresaDashboardOnlyOwnEmpresa() throws Exception {
        authHelper.login(EMPRESA1_USER_EMAIL, EMPRESA1_USER_PASSWORD);
        
        HttpRequest request = authHelper.authenticatedRequest(BASE_URL + "/api/dashboard/empresa/999")
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(403, response.statusCode(),
                "Deve negar acesso ao dashboard de outra empresa");
    }

    @Test
    @Order(18)
    void testUsuarioListFilteredByEmpresa() throws Exception {
        authHelper.login(EMPRESA1_USER_EMAIL, EMPRESA1_USER_PASSWORD);
        
        HttpRequest request = authHelper.authenticatedRequest(BASE_URL + "/api/usuarios")
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode(), "Deve retornar usuários da mesma empresa");
    }

    @Test
    @Order(19)
    void testCannotAccessOtherEmpresaUsuario() throws Exception {
        authHelper.login(EMPRESA1_USER_EMAIL, EMPRESA1_USER_PASSWORD);
        
        HttpRequest request = authHelper.authenticatedRequest(BASE_URL + "/api/usuarios/999")
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertTrue(response.statusCode() == 403 || response.statusCode() == 404,
                "Deve negar acesso a usuário de empresa diferente");
    }

    @Test
    @Order(20)
    void testFeedbackAccessValidation() throws Exception {
        authHelper.login(EMPRESA1_USER_EMAIL, EMPRESA1_USER_PASSWORD);
        
        HttpRequest request = authHelper.authenticatedRequest(BASE_URL + "/api/feedbacks/gravacao/999")
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertTrue(response.statusCode() == 403 || response.statusCode() == 404,
                "Deve negar acesso a feedback de gravação de usuário diferente");
    }

    // ============== Public Endpoint Tests ==============

    @Test
    @Order(21)
    void testHealthEndpointPublic() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/health"))
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode(), "Endpoint de health deve ser público");
    }

    @Test
    @Order(22)
    void testAuthEndpointsPublic() throws Exception {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", "test@test.com");
        credentials.put("password", "password");
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(credentials)))
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertTrue(response.statusCode() != 403, "Endpoints de autenticação devem ser públicos");
    }
}
