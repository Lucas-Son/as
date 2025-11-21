package com.fiap.esoa.salesmind.dto.request;

public record SignupEmpresaRequest(
        String nomeEmpresa,
        String cnpj,
        String email,
        String senha) {
    public boolean isValid() {
        return nomeEmpresa != null && !nomeEmpresa.isBlank() &&
                cnpj != null && !cnpj.isBlank() &&
                email != null && !email.isBlank() &&
                senha != null && !senha.isBlank();
    }

    public String getValidationError() {
        if (nomeEmpresa == null || nomeEmpresa.isBlank()) {
            return "Nome da empresa é obrigatório";
        }
        if (cnpj == null || cnpj.isBlank()) {
            return "CNPJ é obrigatório";
        }
        if (email == null || email.isBlank()) {
            return "Email é obrigatório";
        }
        if (senha == null || senha.isBlank()) {
            return "Senha é obrigatória";
        }
        return null;
    }
}
