package com.fiap.esoa.salesmind.controller;

import com.fiap.esoa.salesmind.dto.request.CreateClienteRequest;
import com.fiap.esoa.salesmind.dto.request.UpdateClienteRequest;
import com.fiap.esoa.salesmind.model.Cliente;
import com.fiap.esoa.salesmind.service.ClienteService;
import com.fiap.esoa.salesmind.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.Optional;

public class ClienteController extends BaseController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        String[] pathParts = path.split("/");
        Long id = pathParts.length > 3 ? parseLongOrNull(pathParts[3]) : null;

        if (id != null && pathParts.length > 4 && "dashboard".equals(pathParts[4])) {
            JsonUtil.sendErrorResponse(exchange, 308, "Use /api/dashboard/clientes/" + id);
            return;
        }

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
            Optional<Cliente> cliente = service.findById(id);
            if (cliente.isEmpty()) {
                JsonUtil.sendErrorResponse(exchange, 404, "Cliente não encontrado");
                return;
            }
            
            if (!validateEmpresaAccess(exchange, cliente.get().getIdEmpresa())) {
                JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Cliente pertence a outra empresa");
                return;
            }
            
            JsonUtil.sendJsonResponse(exchange, 200, cliente.get());
        } else {
            JsonUtil.sendJsonResponse(exchange, 200, service.findByEmpresa(authenticatedEmpresaId));
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        Long authenticatedEmpresaId = getAuthenticatedEmpresaId(exchange);
        
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            JsonUtil.sendErrorResponse(exchange, 415, "Content-Type deve ser application/json");
            return;
        }

        try {
            String body = getRequestBody(exchange);
            CreateClienteRequest request = JsonUtil.fromJson(body, CreateClienteRequest.class);

            if (!request.isValid()) {
                JsonUtil.sendErrorResponse(exchange, 400, request.getValidationError());
                return;
            }

            Cliente cliente = new Cliente();
            cliente.setNome(request.nome());
            cliente.setCpfCnpj(request.cpfCnpj());
            cliente.setTelefone(request.telefone());
            cliente.setEmail(request.email());
            cliente.setSegmento(request.segmento());
            cliente.setIdEmpresa(authenticatedEmpresaId);

            Cliente created = service.create(cliente);
            JsonUtil.sendJsonResponse(exchange, 201, created);

        } catch (Exception e) {
            JsonUtil.sendErrorResponse(exchange, 400, "Invalid request body: " + e.getMessage());
        }
    }

    private void handlePut(HttpExchange exchange, Long id) throws IOException {
        if (id == null) {
            JsonUtil.sendErrorResponse(exchange, 400, "ID é obrigatório para atualização");
            return;
        }

        Optional<Cliente> existing = service.findById(id);
        if (existing.isEmpty()) {
            JsonUtil.sendErrorResponse(exchange, 404, "Cliente não encontrado");
            return;
        }
        
        if (!validateEmpresaAccess(exchange, existing.get().getIdEmpresa())) {
            JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Cliente pertence a outra empresa");
            return;
        }

        try {
            String body = getRequestBody(exchange);
            UpdateClienteRequest request = JsonUtil.fromJson(body, UpdateClienteRequest.class);
            
            if (!request.hasValidUpdate()) {
                JsonUtil.sendErrorResponse(exchange, 400, request.getValidationError());
                return;
            }
            
            Cliente cliente = existing.get();
            
            if (request.nome() != null && !request.nome().trim().isEmpty()) {
                cliente.setNome(request.nome());
            }
            if (request.cpfCnpj() != null && !request.cpfCnpj().trim().isEmpty()) {
                cliente.setCpfCnpj(request.cpfCnpj());
            }
            if (request.telefone() != null) {
                cliente.setTelefone(request.telefone());
            }
            if (request.email() != null) {
                cliente.setEmail(request.email());
            }
            if (request.segmento() != null) {
                cliente.setSegmento(request.segmento());
            }

            Cliente updated = service.create(cliente);
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

        String role = getAuthenticatedRole(exchange);
        if (!"ADMIN".equals(role) && !"GERENTE".equals(role)) {
            JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Apenas administradores ou gerentes podem deletar clientes");
            return;
        }

        Optional<Cliente> existing = service.findById(id);
        if (existing.isEmpty()) {
            JsonUtil.sendErrorResponse(exchange, 404, "Cliente não encontrado");
            return;
        }
        
        if (!validateEmpresaAccess(exchange, existing.get().getIdEmpresa())) {
            JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Cliente pertence a outra empresa");
            return;
        }

        // Check if cliente has gravacoes
        long gravacoes = service.countGravacoesByCliente(id);
        if (gravacoes > 0) {
            JsonUtil.sendErrorResponse(exchange, 409, 
                "Não é possível excluir o cliente: existem " + gravacoes + " gravação(ões) associada(s)");
            return;
        }

        service.deleteById(id);
        addCorsHeaders(exchange);
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
