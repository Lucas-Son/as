package com.fiap.esoa.salesmind.dto.response;

public record GravacaoStatusResponse(
        Long id,
        String statusProcessamento,
        String transcricao,
        String resumoIA,
        String erroProcessamento) {
}
