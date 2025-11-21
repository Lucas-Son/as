package com.fiap.esoa.salesmind.dto.request;

public record CreateGravacaoRequest(
        Long idCliente,
        String statusVenda) {
    public boolean isValid() {
        return idCliente != null;
    }

    public String getValidationError() {
        if (idCliente == null)
            return "ID do cliente é obrigatório";
        return null;
    }
}
