package com.fiap.esoa.salesmind.dto.request;

public record CreateEmpresaRequest(
        String nomeEmpresa,
        String cnpj) {
    public boolean isValid() {
        return nomeEmpresa != null && !nomeEmpresa.trim().isEmpty()
                && cnpj != null && !cnpj.trim().isEmpty();
    }

    public String getValidationError() {
        if (nomeEmpresa == null || nomeEmpresa.trim().isEmpty()) {
            return "Nome empresa is required";
        }
        if (cnpj == null || cnpj.trim().isEmpty()) {
            return "CNPJ is required";
        }
        return null;
    }
}
