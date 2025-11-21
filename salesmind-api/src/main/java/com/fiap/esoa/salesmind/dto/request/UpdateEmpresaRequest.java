package com.fiap.esoa.salesmind.dto.request;

public record UpdateEmpresaRequest(
        String nomeEmpresa,
        String cnpj) {
    public boolean hasValidUpdate() {
        return (nomeEmpresa != null && !nomeEmpresa.trim().isEmpty())
                || (cnpj != null && !cnpj.trim().isEmpty());
    }

    public String getValidationError() {
        if (!hasValidUpdate()) {
            return "Pelo menos um campo deve ser fornecido para atualização";
        }
        return null;
    }
}
