package com.fiap.esoa.salesmind.controller;

import com.fiap.esoa.salesmind.dto.UsuarioDTO;
import com.fiap.esoa.salesmind.dto.request.SignupEmpresaRequest;
import com.fiap.esoa.salesmind.enums.Funcao;
import com.fiap.esoa.salesmind.model.Empresa;
import com.fiap.esoa.salesmind.model.Usuario;
import com.fiap.esoa.salesmind.service.EmpresaService;
import com.fiap.esoa.salesmind.service.UsuarioService;
import com.fiap.esoa.salesmind.util.JsonUtil;
import com.fiap.esoa.salesmind.util.TransactionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller público para cadastro de empresa
 * Cria empresa e usuário admin em uma única operação
 */
public class SignupController implements HttpHandler {

    private final EmpresaService empresaService;
    private final UsuarioService usuarioService;

    public SignupController(EmpresaService empresaService, UsuarioService usuarioService) {
        this.empresaService = empresaService;
        this.usuarioService = usuarioService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            JsonUtil.sendErrorResponse(exchange, 405, "Método não permitido. Use POST.");
            return;
        }

        handleSignup(exchange);
    }

    private void handleSignup(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            JsonUtil.sendErrorResponse(exchange, 415, "Content-Type deve ser application/json");
            return;
        }

        try {
            String body = getRequestBody(exchange);
            SignupEmpresaRequest request = JsonUtil.fromJson(body, SignupEmpresaRequest.class);

            if (!request.isValid()) {
                JsonUtil.sendErrorResponse(exchange, 400, request.getValidationError());
                return;
            }

            Usuario existingUser = usuarioService.findByEmail(request.email());
            if (existingUser != null) {
                JsonUtil.sendErrorResponse(exchange, 409, 
                    "Email já cadastrado. Use um email diferente.");
                return;
            }

            Optional<Empresa> existingEmpresa = empresaService.findByCnpj(request.cnpj());
            if (existingEmpresa.isPresent()) {
                JsonUtil.sendErrorResponse(exchange, 409, "CNPJ já cadastrado no sistema");
                return;
            }

            Map<String, Object> result = TransactionManager.executeTransaction(conn -> {
                Empresa empresa = new Empresa();
                empresa.setNomeEmpresa(request.nomeEmpresa());
                empresa.setCnpj(request.cnpj());

                Empresa createdEmpresa = empresaService.create(empresa);

                Usuario adminUser = new Usuario();
                adminUser.setNome(request.nomeEmpresa());
                adminUser.setEmail(request.email());
                adminUser.setSenha(request.senha());
                adminUser.setFuncao(Funcao.ADMIN);
                adminUser.setIdEmpresa(createdEmpresa.getId());

                Usuario createdAdmin = usuarioService.create(adminUser);

                Map<String, Object> response = new HashMap<>();
                response.put("empresa", createdEmpresa);
                response.put("adminUser", UsuarioDTO.fromEntity(createdAdmin));
                response.put("message", "Empresa criada com sucesso! Use o email do administrador para fazer login.");

                return response;
            });

            JsonUtil.sendJsonResponse(exchange, 201, result);

        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendErrorResponse(exchange, 500, 
                "Erro ao criar empresa: " + e.getMessage());
        }
    }

    private String getRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            return body.toString();
        }
    }
}
