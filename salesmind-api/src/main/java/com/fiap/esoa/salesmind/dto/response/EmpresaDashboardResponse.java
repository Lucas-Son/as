package com.fiap.esoa.salesmind.dto.response;

public record EmpresaDashboardResponse(
        Long idEmpresa,
        Long totalGravacoes,
        Long totalClientes,
        Double taxaConversao) {
}
