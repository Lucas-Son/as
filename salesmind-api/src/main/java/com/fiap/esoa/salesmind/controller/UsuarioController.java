package com.fiap.esoa.salesmind.controller;

import com.fiap.esoa.salesmind.dto.UsuarioDTO;
import com.fiap.esoa.salesmind.dto.request.CreateUsuarioRequest;
import com.fiap.esoa.salesmind.dto.request.UpdateUsuarioRequest;
import com.fiap.esoa.salesmind.dto.response.UsuarioStatsResponse;
import com.fiap.esoa.salesmind.enums.Funcao;
import com.fiap.esoa.salesmind.model.Usuario;
import com.fiap.esoa.salesmind.service.UsuarioService;
import com.fiap.esoa.salesmind.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UsuarioController extends BaseController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        String[] pathParts = path.split("/");
        Long id = pathParts.length > 3 ? parseLongOrNull(pathParts[3]) : null;

        if (id != null && pathParts.length > 4 && "stats".equals(pathParts[4])) {
            handleStats(exchange, id);
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
            Optional<Usuario> usuario = service.findById(id);
            if (usuario.isEmpty()) {
                JsonUtil.sendErrorResponse(exchange, 404, "Usuário não encontrado");
                return;
            }
            if (!usuario.get().getIdEmpresa().equals(authenticatedEmpresaId)) {
                JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Usuário pertence a outra empresa");
                return;
            }
            UsuarioDTO dto = UsuarioDTO.fromEntity(usuario.get());
            JsonUtil.sendJsonResponse(exchange, 200, dto);
        } else {
            List<Usuario> usuarios = service.findByEmpresa(authenticatedEmpresaId);
            List<UsuarioDTO> dtos = usuarios.stream()
                    .map(UsuarioDTO::fromEntity)
                    .collect(Collectors.toList());
            JsonUtil.sendJsonResponse(exchange, 200, dtos);
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            JsonUtil.sendErrorResponse(exchange, 415, "Content-Type deve ser application/json");
            return;
        }

        Long authenticatedEmpresaId = getAuthenticatedEmpresaId(exchange);
        String authenticatedRole = getAuthenticatedRole(exchange);

        if (!"ADMIN".equals(authenticatedRole)) {
            JsonUtil.sendErrorResponse(exchange, 403, 
                "Acesso negado: Apenas administradores da empresa podem criar usuários");
            return;
        }

        try {
            String body = getRequestBody(exchange);
            CreateUsuarioRequest request = JsonUtil.fromJson(body, CreateUsuarioRequest.class);

            if (!request.isValid()) {
                JsonUtil.sendErrorResponse(exchange, 400, request.getValidationError());
                return;
            }

            if (Funcao.ADMIN.equals(request.funcao())) {
                JsonUtil.sendErrorResponse(exchange, 400, 
                    "Não é possível criar outro usuário ADMIN. Apenas um ADMIN por empresa.");
                return;
            }

            Usuario usuario = new Usuario();
            usuario.setNome(request.nome());
            usuario.setEmail(request.email());
            usuario.setSenha(request.senha());
            usuario.setFuncao(request.funcao());
            usuario.setIdEmpresa(authenticatedEmpresaId);

            Usuario created = service.create(usuario);
            UsuarioDTO dto = UsuarioDTO.fromEntity(created);
            JsonUtil.sendJsonResponse(exchange, 201, dto);

        } catch (Exception e) {
            JsonUtil.sendErrorResponse(exchange, 400, "Corpo da requisição inválido: " + e.getMessage());
        }
    }

    private void handlePut(HttpExchange exchange, Long id) throws IOException {
        if (id == null) {
            JsonUtil.sendErrorResponse(exchange, 400, "ID é obrigatório para atualização");
            return;
        }

        Long authenticatedEmpresaId = getAuthenticatedEmpresaId(exchange);
        Optional<Usuario> existing = service.findById(id);

        if (existing.isEmpty()) {
            JsonUtil.sendErrorResponse(exchange, 404, "Usuário não encontrado");
            return;
        }

        if (!existing.get().getIdEmpresa().equals(authenticatedEmpresaId)) {
            JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Usuário pertence a outra empresa");
            return;
        }

        try {
            String body = getRequestBody(exchange);
            UpdateUsuarioRequest request = JsonUtil.fromJson(body, UpdateUsuarioRequest.class);
            
            if (!request.hasValidUpdate()) {
                JsonUtil.sendErrorResponse(exchange, 400, request.getValidationError());
                return;
            }
            
            Usuario usuario = existing.get();
            
            if (request.nome() != null && !request.nome().trim().isEmpty()) {
                usuario.setNome(request.nome());
            }
            if (request.email() != null && !request.email().trim().isEmpty()) {
                usuario.setEmail(request.email());
            }
            
            if (request.funcao() != null) {
                if (!isAdmin(exchange)) {
                    JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Apenas administradores podem alterar a função do usuário");
                    return;
                }
                
                if (Funcao.ADMIN.equals(request.funcao())) {
                    JsonUtil.sendErrorResponse(exchange, 400, "Não é possível alterar função para ADMIN");
                    return;
                }
                
                usuario.setFuncao(request.funcao());
            }

            Usuario updated = service.create(usuario);
            UsuarioDTO dto = UsuarioDTO.fromEntity(updated);
            JsonUtil.sendJsonResponse(exchange, 200, dto);

        } catch (Exception e) {
            JsonUtil.sendErrorResponse(exchange, 400, "Corpo da requisição inválido: " + e.getMessage());
        }
    }

    private void handleStats(HttpExchange exchange, Long id) throws IOException {
        Long authenticatedEmpresaId = getAuthenticatedEmpresaId(exchange);
        Optional<Usuario> usuario = service.findById(id);

        if (usuario.isEmpty()) {
            JsonUtil.sendErrorResponse(exchange, 404, "Usuário não encontrado");
            return;
        }

        if (!usuario.get().getIdEmpresa().equals(authenticatedEmpresaId)) {
            JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Usuário pertence a outra empresa");
            return;
        }

        long vendasFechadas = service.getVendasFechadas(id);
        UsuarioStatsResponse response = new UsuarioStatsResponse(id, vendasFechadas);
        JsonUtil.sendJsonResponse(exchange, 200, response);
    }

    private void handleDelete(HttpExchange exchange, Long id) throws IOException {
        if (id == null) {
            JsonUtil.sendErrorResponse(exchange, 400, "ID é obrigatório para exclusão");
            return;
        }

        if (!isAdmin(exchange)) {
            JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Apenas administradores podem deletar usuários");
            return;
        }

        Long authenticatedEmpresaId = getAuthenticatedEmpresaId(exchange);
        Optional<Usuario> existing = service.findById(id);

        if (existing.isEmpty()) {
            JsonUtil.sendErrorResponse(exchange, 404, "Usuário não encontrado");
            return;
        }

        if (!existing.get().getIdEmpresa().equals(authenticatedEmpresaId)) {
            JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Usuário pertence a outra empresa");
            return;
        }

        if (Funcao.ADMIN.equals(existing.get().getFuncao())) {
            JsonUtil.sendErrorResponse(exchange, 400, "Não é possível deletar o usuário ADMIN da empresa");
            return;
        }

        long gravacoes = service.countGravacoesByUsuario(id);
        if (gravacoes > 0) {
            JsonUtil.sendErrorResponse(exchange, 409, 
                "Não é possível excluir o usuário: existem " + gravacoes + " gravação(ões) associada(s)");
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
