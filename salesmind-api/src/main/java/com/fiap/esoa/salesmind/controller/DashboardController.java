package com.fiap.esoa.salesmind.controller;

import com.fiap.esoa.salesmind.dto.response.EmpresaDashboardResponse;
import com.fiap.esoa.salesmind.model.Cliente;
import com.fiap.esoa.salesmind.repository.ClienteRepository;
import com.fiap.esoa.salesmind.repository.GravacaoCallRepository;
import com.fiap.esoa.salesmind.dto.response.ClienteDashboardResponse;
import com.fiap.esoa.salesmind.service.ClienteService;
import com.fiap.esoa.salesmind.service.DashboardService;
import com.fiap.esoa.salesmind.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class DashboardController extends BaseController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String[] pathParts = path.split("/");

        // GET /api/dashboard/empresa/{id}
        if (pathParts.length >= 5 && "empresa".equals(pathParts[3])) {
            Long id = parseLongOrNull(pathParts[4]);
            if (id != null) {
                handleEmpresaDashboard(exchange, id);
            } else {
                JsonUtil.sendErrorResponse(exchange, 400, "ID de empresa inválido");
            }
            return;
        }

        // GET /api/dashboard/clientes/{id} - rota alternativa
        if (pathParts.length >= 5 && "clientes".equals(pathParts[3])) {
            Long id = parseLongOrNull(pathParts[4]);
            if (id != null) {
                handleClienteDashboard(exchange, id);
            } else {
                JsonUtil.sendErrorResponse(exchange, 400, "ID de cliente inválido");
            }
            return;
        }

        JsonUtil.sendErrorResponse(exchange, 404,
                "Endpoint de dashboard não encontrado. Use /api/dashboard/empresa/{id} ou /api/dashboard/clientes/{id}");
    }

    private void handleEmpresaDashboard(HttpExchange exchange, Long id) throws IOException {
        if (!isAdmin(exchange)) {
            JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Apenas administradores podem acessar o dashboard da empresa");
            return;
        }

        Long authenticatedEmpresaId = getAuthenticatedEmpresaId(exchange);

        if (!id.equals(authenticatedEmpresaId)) {
            JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Não é possível acessar dashboard de outra empresa");
            return;
        }

        try {
            Map<String, Object> dashboard = service.getEmpresaDashboard(id);

            Object totalGravObj = dashboard.get("totalGravacoes");
            Object totalCliObj = dashboard.get("totalClientes");
            Object taxaConvObj = dashboard.get("taxaConversao");
            
            Long totalGravacoes = 0L;
            Long totalClientes = 0L;
            Double taxaConversao = 0.0;
            
            if (totalGravObj instanceof Integer) {
                totalGravacoes = ((Integer) totalGravObj).longValue();
            } else if (totalGravObj instanceof Long) {
                totalGravacoes = (Long) totalGravObj;
            }
            
            if (totalCliObj instanceof Integer) {
                totalClientes = ((Integer) totalCliObj).longValue();
            } else if (totalCliObj instanceof Long) {
                totalClientes = (Long) totalCliObj;
            }
            
            if (taxaConvObj instanceof Double) {
                taxaConversao = (Double) taxaConvObj;
            } else if (taxaConvObj instanceof Float) {
                taxaConversao = ((Float) taxaConvObj).doubleValue();
            }

            EmpresaDashboardResponse response = new EmpresaDashboardResponse(
                    id, totalGravacoes, totalClientes, taxaConversao);

            JsonUtil.sendJsonResponse(exchange, 200, response);
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendErrorResponse(exchange, 500, "Erro ao gerar dashboard: " + e.getMessage());
        }
    }

    private void handleClienteDashboard(HttpExchange exchange, Long id) throws IOException {
        Long authenticatedEmpresaId = getAuthenticatedEmpresaId(exchange);
        
        try {
            ClienteService clienteService = 
                new ClienteService(
                    new ClienteRepository(),
                    new GravacaoCallRepository());
            
            Optional<Cliente> cliente = clienteService.findById(id);
            if (cliente.isEmpty()) {
                JsonUtil.sendErrorResponse(exchange, 404, "Cliente não encontrado");
                return;
            }
            
            if (!cliente.get().getIdEmpresa().equals(authenticatedEmpresaId)) {
                JsonUtil.sendErrorResponse(exchange, 403, "Acesso negado: Cliente pertence a outra empresa");
                return;
            }
            
            Map<String, Object> dashboard = service.getClienteDashboard(id);

            Integer totalGravacoes = (Integer) dashboard.get("totalGravacoes");
            Integer vendasFechadas = (Integer) dashboard.get("vendasFechadas");

            ClienteDashboardResponse response = new ClienteDashboardResponse(
                    id,
                    totalGravacoes != null ? totalGravacoes.longValue() : 0L,
                    vendasFechadas != null ? vendasFechadas.longValue() : 0L);

            JsonUtil.sendJsonResponse(exchange, 200, response);
        } catch (Exception e) {
            JsonUtil.sendErrorResponse(exchange, 500, "Erro ao gerar dashboard: " + e.getMessage());
        }
    }

    private Long parseLongOrNull(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
