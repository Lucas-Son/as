package com.fiap.esoa.salesmind.dto.request;

public record CreateClienteRequest(
        String nome,
        String cpfCnpj,
        String telefone,
        String email,
        String segmento) {
    public boolean isValid() {
        return nome != null && !nome.trim().isEmpty();
    }

    public String getValidationError() {
        if (nome == null || nome.trim().isEmpty()) {
            return "Nome é obrigatório";
        }
        return null;
    }
}
