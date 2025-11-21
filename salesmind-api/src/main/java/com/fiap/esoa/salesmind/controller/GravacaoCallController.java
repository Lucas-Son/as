package com.fiap.esoa.salesmind.controller;

import com.fiap.esoa.salesmind.dto.GravacaoCallDTO;
import com.fiap.esoa.salesmind.dto.request.CreateGravacaoRequest;
import com.fiap.esoa.salesmind.dto.request.UpdateGravacaoRequest;
import com.fiap.esoa.salesmind.enums.StatusProcessamento;
import com.fiap.esoa.salesmind.enums.StatusVenda;
import com.fiap.esoa.salesmind.model.GravacaoCall;
import com.fiap.esoa.salesmind.model.Cliente;
import com.fiap.esoa.salesmind.service.GravacaoCallService;
import com.fiap.esoa.salesmind.service.ClienteService;
import com.fiap.esoa.salesmind.util.FileUploadUtil;
import com.fiap.esoa.salesmind.util.JsonUtil;
import com.fiap.esoa.salesmind.util.MultipartParser;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class GravacaoCallController extends BaseController {

    private final GravacaoCallService service;
    private final ClienteService clienteService;

    public GravacaoCallController(GravacaoCallService service, ClienteService clienteService) {
        this.service = service;
        this.clienteService = clienteService;
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        String[] pathParts = path.split("/");

        // POST /api/gravacoes/upload
        if (method.equals("POST") && pathParts.length > 3 && "upload".equals(pathParts[3])) {
            handleUpload(exchange);
            return;
        }

        // GET /api/gravacoes/{id}/status
        if (method.equals("GET") && pathParts.length > 4 && "status".equals(pathParts[4])) {
            Long id = parseLongOrNull(pathParts[3]);
            if (id != null) {
                handleStatus(exchange, id);
                return;
            }
        }

        Long id = pathParts.length > 3 ? parseLongOrNull(pathParts[3]) : null;

        switch (method) {
            case "GET" -> handleGet(exchange, id);
            case "POST" -> handlePost(exchange);
            case "PUT" -> handlePut(exchange, id);
            default -> sendError(exchange, 405, "Method not allowed");
        }
    }

    private void handleGet(HttpExchange exchange, Long id) throws IOException {
        Long authenticatedUserId = getAuthenticatedUserId(exchange);
        
        if (id != null) {
            Optional<GravacaoCall> gravacao = service.findById(id);
            if (gravacao.isEmpty()) {
                JsonUtil.sendErrorResponse(exchange, 404, "Gravação não encontrada");
                return;
            }
            
            if (!gravacao.get().getIdUsuario().equals(authenticatedUserId)) {
                JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Gravação pertence a outro usuário");
                return;
            }
            
            GravacaoCallDTO dto = GravacaoCallDTO.fromEntity(gravacao.get());
            JsonUtil.sendJsonResponse(exchange, 200, dto);
        } else {
            List<GravacaoCall> gravacoes = service.findByUsuario(authenticatedUserId);
            List<GravacaoCallDTO> dtos = gravacoes.stream()
                    .map(GravacaoCallDTO::fromEntity)
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

        Long authenticatedUserId = getAuthenticatedUserId(exchange);
        Long authenticatedEmpresaId = getAuthenticatedEmpresaId(exchange);

        try {
            String body = getRequestBody(exchange);
            CreateGravacaoRequest request = JsonUtil.fromJson(body, CreateGravacaoRequest.class);

            if (!request.isValid()) {
                JsonUtil.sendErrorResponse(exchange, 400, request.getValidationError());
                return;
            }

            Optional<Cliente> cliente = clienteService.findById(request.idCliente());
            if (cliente.isEmpty()) {
                JsonUtil.sendErrorResponse(exchange, 404, "Cliente não encontrado");
                return;
            }
            if (!cliente.get().getIdEmpresa().equals(authenticatedEmpresaId)) {
                JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Cliente pertence a outra empresa");
                return;
            }

            GravacaoCall gravacao = new GravacaoCall();
            gravacao.setIdUsuario(authenticatedUserId);
            gravacao.setIdCliente(request.idCliente());
            if (request.statusVenda() != null) {
                gravacao.setStatusVenda(StatusVenda.valueOf(request.statusVenda()));
            }

            GravacaoCall created = service.save(gravacao);
            GravacaoCallDTO dto = GravacaoCallDTO.fromEntity(created);
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

        Long authenticatedUserId = getAuthenticatedUserId(exchange);
        Optional<GravacaoCall> existing = service.findById(id);

        if (existing.isEmpty()) {
            JsonUtil.sendErrorResponse(exchange, 404, "Gravação não encontrada");
            return;
        }

        if (!existing.get().getIdUsuario().equals(authenticatedUserId)) {
            JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Gravação pertence a outro usuário");
            return;
        }

        try {
            String body = getRequestBody(exchange);
            UpdateGravacaoRequest request = JsonUtil.fromJson(body, UpdateGravacaoRequest.class);
            
            if (!request.hasValidUpdate()) {
                JsonUtil.sendErrorResponse(exchange, 400, request.getValidationError());
                return;
            }
            
            GravacaoCall gravacao = existing.get();
            
            if (request.statusVenda() != null && !request.statusVenda().trim().isEmpty()) {
                gravacao.setStatusVenda(StatusVenda.valueOf(request.statusVenda()));
            }
            if (request.transcricao() != null && !request.transcricao().trim().isEmpty()) {
                gravacao.setTranscricao(request.transcricao());
            }
            if (request.resumoIA() != null && !request.resumoIA().trim().isEmpty()) {
                gravacao.setResumoIA(request.resumoIA());
            }

            GravacaoCall updated = service.save(gravacao);
            GravacaoCallDTO dto = GravacaoCallDTO.fromEntity(updated);
            JsonUtil.sendJsonResponse(exchange, 200, dto);

        } catch (IllegalArgumentException e) {
            JsonUtil.sendErrorResponse(exchange, 400, "Status de venda inválido: " + e.getMessage());
        } catch (Exception e) {
            JsonUtil.sendErrorResponse(exchange, 400, "Corpo da requisição inválido: " + e.getMessage());
        }
    }

    private void handleUpload(HttpExchange exchange) throws IOException {
        Long authenticatedUserId = getAuthenticatedUserId(exchange);
        Long authenticatedEmpresaId = getAuthenticatedEmpresaId(exchange);

        try {
            MultipartParser.FormData formData = MultipartParser.parse(exchange);

            String idClienteStr = formData.getField("idCliente");
            MultipartParser.FileData audioFile = formData.getFile("audioFile");

            if (idClienteStr == null || audioFile == null) {
                JsonUtil.sendErrorResponse(exchange, 400,
                        "Missing required fields: idCliente and audioFile are required");
                return;
            }

            Long idCliente = parseLongOrNull(idClienteStr);

            if (idCliente == null) {
                JsonUtil.sendErrorResponse(exchange, 400,
                        "Invalid idCliente: must be a valid number");
                return;
            }

            Optional<Cliente> cliente = clienteService.findById(idCliente);
            if (cliente.isEmpty()) {
                JsonUtil.sendErrorResponse(exchange, 404, "Cliente not found");
                return;
            }
            if (!cliente.get().getIdEmpresa().equals(authenticatedEmpresaId)) {
                JsonUtil.sendErrorResponse(exchange, 403, "Access denied: Cliente belongs to another empresa");
                return;
            }

            Long idUsuario = authenticatedUserId;

            String originalFilename = audioFile.getFilename();
            long fileSize = audioFile.getSize();

            if (originalFilename == null || originalFilename.isEmpty()) {
                JsonUtil.sendErrorResponse(exchange, 400, "No file uploaded");
                return;
            }

            String savedFilePath;
            try (java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(audioFile.getData())) {
                FileUploadUtil fileUtil = new FileUploadUtil("uploads");
                savedFilePath = fileUtil.saveFile(inputStream, originalFilename, idUsuario, idCliente);
            } catch (IllegalArgumentException e) {
                JsonUtil.sendErrorResponse(exchange, 400, e.getMessage());
                return;
            }

            int estimatedDuration = (int) (fileSize / 16000);

            GravacaoCall gravacao = new GravacaoCall();
            gravacao.setIdUsuario(idUsuario);
            gravacao.setIdCliente(idCliente);
            gravacao.setAudioFilename(originalFilename);
            gravacao.setAudioUrl(savedFilePath);
            gravacao.setDuracaoSegundos(estimatedDuration);
            gravacao.setStatusProcessamento(StatusProcessamento.UPLOADING);
            gravacao.setStatusVenda(StatusVenda.PENDENTE);

            GravacaoCall saved = service.save(gravacao);

            service.processAudioAsync(saved.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("id", saved.getId());
            response.put("status", saved.getStatusProcessamento());
            response.put("message", "Audio upload successful. Processing started asynchronously.");
            response.put("audioFilename", saved.getAudioFilename());
            response.put("audioUrl", saved.getAudioUrl());
            response.put("estimatedDuration", estimatedDuration + "s");
            response.put("checkStatusAt", "/api/gravacoes/" + saved.getId() + "/status");

            JsonUtil.sendJsonResponse(exchange, 202, response);

        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendErrorResponse(exchange, 500, "Upload failed: " + e.getMessage());
        }
    }

    private void handleStatus(HttpExchange exchange, Long id) throws IOException {
        Long authenticatedUserId = getAuthenticatedUserId(exchange);
        Optional<GravacaoCall> gravacao = service.findById(id);

        if (gravacao.isEmpty()) {
            JsonUtil.sendErrorResponse(exchange, 404, "Gravação não encontrada");
            return;
        }
        
        if (!gravacao.get().getIdUsuario().equals(authenticatedUserId)) {
            JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Gravação pertence a outro usuário");
            return;
        }

        GravacaoCall g = gravacao.get();
        Map<String, Object> status = new HashMap<>();
        status.put("id", g.getId());
        status.put("statusProcessamento", g.getStatusProcessamento());
        status.put("statusVenda", g.getStatusVenda());
        status.put("hasTranscricao", g.getTranscricao() != null);
        status.put("hasResumo", g.getResumoIA() != null);
        status.put("hasFeedback", g.getFeedback() != null);
        status.put("erroProcessamento", g.getErroProcessamento());

        JsonUtil.sendJsonResponse(exchange, 200, status);
    }

    private Long parseLongOrNull(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
