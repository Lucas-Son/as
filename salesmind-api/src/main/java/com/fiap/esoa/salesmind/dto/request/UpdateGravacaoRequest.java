package com.fiap.esoa.salesmind.dto.request;

import com.fiap.esoa.salesmind.enums.StatusVenda;

public record UpdateGravacaoRequest(
        String statusVenda,
        String transcricao,
        String resumoIA) {
    
    public boolean hasValidUpdate() {
        return (statusVenda != null && !statusVenda.trim().isEmpty())
                || (transcricao != null && !transcricao.trim().isEmpty())
                || (resumoIA != null && !resumoIA.trim().isEmpty());
    }

    public String getValidationError() {
        if (!hasValidUpdate()) {
            return "Pelo menos um campo deve ser fornecido para atualização";
        }
        
        if (statusVenda != null) {
            try {
                StatusVenda.valueOf(statusVenda);
            } catch (IllegalArgumentException e) {
                return "Status de venda inválido. Valores permitidos: PENDENTE, FECHADO, PERDIDO, EM_NEGOCIACAO";
            }
        }
        
        return null;
    }
}
