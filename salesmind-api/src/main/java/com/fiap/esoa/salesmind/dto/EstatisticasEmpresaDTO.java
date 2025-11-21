package com.fiap.esoa.salesmind.dto;

public record EstatisticasEmpresaDTO(
        Long idEmpresa,
        Long totalUsuarios,
        Long totalClientes,
        Long totalGravacoes,
        Long vendasFechadas,
        Double taxaConversao
) {
}
