package com.fiap.esoa.salesmind.dto.request;

public record UpdateClienteRequest(
        String nome,
        String cpfCnpj,
        String telefone,
        String email,
        String segmento) {
    public boolean hasValidUpdate() {
        return (nome != null && !nome.trim().isEmpty())
                || (cpfCnpj != null && !cpfCnpj.trim().isEmpty())
                || (telefone != null && !telefone.trim().isEmpty())
                || (email != null && !email.trim().isEmpty())
                || (segmento != null && !segmento.trim().isEmpty());
    }

    public String getValidationError() {
        if (!hasValidUpdate()) {
            return "Pelo menos um campo deve ser fornecido para atualização";
        }
        return null;
    }
}
