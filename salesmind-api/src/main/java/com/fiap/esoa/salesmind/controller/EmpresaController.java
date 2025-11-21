package com.fiap.esoa.salesmind.controller;

import com.fiap.esoa.salesmind.dto.request.CreateEmpresaRequest;
import com.fiap.esoa.salesmind.dto.request.UpdateEmpresaRequest;
import com.fiap.esoa.salesmind.model.Empresa;
import com.fiap.esoa.salesmind.service.EmpresaService;
import com.fiap.esoa.salesmind.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.Optional;

public class EmpresaController extends BaseController {

    private final EmpresaService service;

    public EmpresaController(EmpresaService service) {
        this.service = service;
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        String[] pathParts = path.split("/");
        Long id = pathParts.length > 3 ? parseLongOrNull(pathParts[3]) : null;

        switch (method) {
            case "GET" -> handleGet(exchange, id);
            case "POST" -> handlePost(exchange);
            case "PUT" -> handlePut(exchange, id);
            case "DELETE" -> handleDelete(exchange, id);
            default -> sendError(exchange, 405, "Method not allowed");
        }
    }

    private void handleGet(HttpExchange exchange, Long id) throws IOException {
        Long authenticatedEmpresaId = getAuthenticatedEmpresaId(exchange);

        if (id != null) {
            // GET /api/empresas/{id}
            if (!id.equals(authenticatedEmpresaId)) {
                JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Não é possível acessar outra empresa");
                return;
            }
            Optional<Empresa> empresa = service.findById(id);
            if (empresa.isPresent()) {
                JsonUtil.sendJsonResponse(exchange, 200, empresa.get());
            } else {
                JsonUtil.sendErrorResponse(exchange, 404, "Empresa não encontrada");
            }
        } else {
            // GET /api/empresas
            Optional<Empresa> empresa = service.findById(authenticatedEmpresaId);
            if (empresa.isPresent()) {
                JsonUtil.sendJsonResponse(exchange, 200, java.util.Collections.singletonList(empresa.get()));
            } else {
                JsonUtil.sendJsonResponse(exchange, 200, java.util.Collections.emptyList());
            }
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        if (!isAdmin(exchange)) {
            JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Apenas administradores podem criar empresas");
            return;
        }

        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            JsonUtil.sendErrorResponse(exchange, 415, "Content-Type deve ser application/json");
            return;
        }

        try {
            String body = getRequestBody(exchange);
            CreateEmpresaRequest request = JsonUtil.fromJson(body, CreateEmpresaRequest.class);

            if (!request.isValid()) {
                JsonUtil.sendErrorResponse(exchange, 400, request.getValidationError());
                return;
            }

            Optional<Empresa> existingEmpresa = service.findByCnpj(request.cnpj());
            if (existingEmpresa.isPresent()) {
                JsonUtil.sendErrorResponse(exchange, 409, "CNPJ já cadastrado no sistema");
                return;
            }

            Empresa empresa = new Empresa();
            empresa.setNomeEmpresa(request.nomeEmpresa());
            empresa.setCnpj(request.cnpj());

            Empresa created = service.create(empresa);
            JsonUtil.sendJsonResponse(exchange, 201, created);

        } catch (Exception e) {
            JsonUtil.sendErrorResponse(exchange, 400, "Corpo da requisição inválido: " + e.getMessage());
        }
    }

    private void handlePut(HttpExchange exchange, Long id) throws IOException {
        if (id == null) {
            JsonUtil.sendErrorResponse(exchange, 400, "ID é obrigatório para atualização");
            return;
        }

        if (!isAdmin(exchange)) {
            JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Apenas administradores podem atualizar a empresa");
            return;
        }

        Long authenticatedEmpresaId = getAuthenticatedEmpresaId(exchange);

        if (!id.equals(authenticatedEmpresaId)) {
            JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Não é possível atualizar outra empresa");
            return;
        }

        Optional<Empresa> existing = service.findById(id);
        if (existing.isEmpty()) {
            JsonUtil.sendErrorResponse(exchange, 404, "Empresa não encontrada");
            return;
        }

        try {
            String body = getRequestBody(exchange);
            UpdateEmpresaRequest request = JsonUtil.fromJson(body, UpdateEmpresaRequest.class);
            
            if (!request.hasValidUpdate()) {
                JsonUtil.sendErrorResponse(exchange, 400, request.getValidationError());
                return;
            }
            
            Empresa empresa = existing.get();

            if (request.nomeEmpresa() != null && !request.nomeEmpresa().trim().isEmpty()) {
                empresa.setNomeEmpresa(request.nomeEmpresa());
            }
            if (request.cnpj() != null && !request.cnpj().trim().isEmpty()) {
                if (!request.cnpj().equals(empresa.getCnpj())) {
                    Optional<Empresa> existingByCnpj = service.findByCnpj(request.cnpj());
                    if (existingByCnpj.isPresent()) {
                        JsonUtil.sendErrorResponse(exchange, 409, "CNPJ já cadastrado no sistema");
                        return;
                    }
                }
                empresa.setCnpj(request.cnpj());
            }

            Empresa updated = service.create(empresa);
            JsonUtil.sendJsonResponse(exchange, 200, updated);

        } catch (Exception e) {
            JsonUtil.sendErrorResponse(exchange, 400, "Corpo da requisição inválido: " + e.getMessage());
        }
    }

    private void handleDelete(HttpExchange exchange, Long id) throws IOException {
        if (id == null) {
            JsonUtil.sendErrorResponse(exchange, 400, "ID é obrigatório para exclusão");
            return;
        }

        if (!isAdmin(exchange)) {
            JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Apenas administradores podem deletar a empresa");
            return;
        }

        Long authenticatedEmpresaId = getAuthenticatedEmpresaId(exchange);

        if (!id.equals(authenticatedEmpresaId)) {
            JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Não é possível excluir outra empresa");
            return;
        }

        Optional<Empresa> empresa = service.findById(id);
        if (empresa.isEmpty()) {
            JsonUtil.sendErrorResponse(exchange, 404, "Empresa não encontrada");
            return;
        }

        if (service.hasDependencies(id)) {
            JsonUtil.sendErrorResponse(exchange, 409, 
                "Não é possível excluir a empresa: existem usuários, clientes ou gravações associados");
            return;
        }

        service.delete(id);
        addCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(204, -1);
    }

    private Long parseLongOrNull(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
