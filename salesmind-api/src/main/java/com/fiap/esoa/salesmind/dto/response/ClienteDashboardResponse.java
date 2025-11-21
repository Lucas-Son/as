package com.fiap.esoa.salesmind.dto.response;

public record ClienteDashboardResponse(
        Long idCliente,
        Long totalGravacoes,
        Long vendasFechadas) {
}
