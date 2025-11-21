package com.fiap.esoa.salesmind.controller;

import com.fiap.esoa.salesmind.dto.FeedbackIADTO;
import com.fiap.esoa.salesmind.model.FeedbackIA;
import com.fiap.esoa.salesmind.service.FeedbackIAService;
import com.fiap.esoa.salesmind.service.GravacaoCallService;
import com.fiap.esoa.salesmind.model.GravacaoCall;
import com.fiap.esoa.salesmind.util.CacheManager;
import com.fiap.esoa.salesmind.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FeedbackIAController extends BaseController {

    private final FeedbackIAService service;
    private final GravacaoCallService gravacaoService;
    private final CacheManager<Long, FeedbackIADTO> cache;

    public FeedbackIAController(FeedbackIAService service, GravacaoCallService gravacaoService) {
        this.service = service;
        this.gravacaoService = gravacaoService;
        this.cache = new CacheManager<>(15);
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        // GET /api/feedbacks/gravacao/{id}
        if ("GET".equals(method) && path.matches("/api/feedbacks/gravacao/\\d+")) {
            handleGetByGravacao(exchange);
            return;
        }

        // GET /api/feedbacks/{id}
        if ("GET".equals(method) && path.matches("/api/feedbacks/\\d+")) {
            handleGetById(exchange);
            return;
        }

        // GET /api/feedbacks
        if ("GET".equals(method) && path.equals("/api/feedbacks")) {
            handleGetAll(exchange);
            return;
        }

        JsonUtil.sendErrorResponse(exchange, 404, "Endpoint não encontrado");
    }

    private void handleGetByGravacao(HttpExchange exchange) throws IOException {
        Long authenticatedUserId = getAuthenticatedUserId(exchange);

        try {
            String path = exchange.getRequestURI().getPath();
            Long gravacaoId = Long.parseLong(path.substring(path.lastIndexOf('/') + 1));

            Optional<GravacaoCall> gravacao = gravacaoService.findById(gravacaoId);
            if (gravacao.isEmpty()) {
                JsonUtil.sendErrorResponse(exchange, 404, "Gravação não encontrada");
                return;
            }
            if (!gravacao.get().getIdUsuario().equals(authenticatedUserId)) {
                JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Gravação pertence a outro usuário");
                return;
            }

            FeedbackIADTO cachedDto = cache.get(gravacaoId);
            if (cachedDto != null) {
                System.out.println("Cache hit for gravacao: " + gravacaoId);
                JsonUtil.sendJsonResponse(exchange, 200, cachedDto);
                return;
            }

            Optional<FeedbackIA> feedback = service.findByGravacao(gravacaoId);
            if (feedback.isEmpty()) {
                JsonUtil.sendErrorResponse(exchange, 404, "Feedback not found for gravacao: " + gravacaoId);
                return;
            }

            FeedbackIADTO dto = FeedbackIADTO.fromEntity(feedback.get());
            
            cache.put(gravacaoId, dto);
            System.out.println("Cached feedback for gravacao: " + gravacaoId);

            JsonUtil.sendJsonResponse(exchange, 200, dto);

        } catch (NumberFormatException e) {
            JsonUtil.sendErrorResponse(exchange, 400, "Invalid gravacao ID");
        } catch (Exception e) {
            System.err.println("Error getting feedback by gravacao: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleGetById(HttpExchange exchange) throws IOException {
        Long authenticatedUserId = getAuthenticatedUserId(exchange);

        try {
            String path = exchange.getRequestURI().getPath();
            Long id = Long.parseLong(path.substring(path.lastIndexOf('/') + 1));

            Optional<FeedbackIA> feedback = service.findById(id);
            if (feedback.isEmpty()) {
                JsonUtil.sendErrorResponse(exchange, 404, "Feedback não encontrado");
                return;
            }

            Optional<GravacaoCall> gravacao = gravacaoService.findById(feedback.get().getIdGravacao());
            if (gravacao.isEmpty() || !gravacao.get().getIdUsuario().equals(authenticatedUserId)) {
                JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Feedback pertence a outro usuário");
                return;
            }

            FeedbackIADTO dto = FeedbackIADTO.fromEntity(feedback.get());
            JsonUtil.sendJsonResponse(exchange, 200, dto);

        } catch (NumberFormatException e) {
            JsonUtil.sendErrorResponse(exchange, 400, "ID de feedback inválido");
        } catch (Exception e) {
            System.err.println("Error getting feedback: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendErrorResponse(exchange, 500, "Erro interno do servidor: " + e.getMessage());
        }
    }

    private void handleGetAll(HttpExchange exchange) throws IOException {
        if (!isAdmin(exchange)) {
            JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Apenas administradores podem listar todos os feedbacks");
            return;
        }

        Long authenticatedEmpresaId = getAuthenticatedEmpresaId(exchange);

        try {
            List<FeedbackIA> feedbacks = service.findByEmpresa(authenticatedEmpresaId);
            List<FeedbackIADTO> dtos = feedbacks.stream()
                    .map(FeedbackIADTO::fromEntity)
                    .collect(Collectors.toList());

            JsonUtil.sendJsonResponse(exchange, 200, dtos);

        } catch (Exception e) {
            System.err.println("Error getting all feedbacks: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendErrorResponse(exchange, 500, "Erro interno do servidor: " + e.getMessage());
        }
    }
}

