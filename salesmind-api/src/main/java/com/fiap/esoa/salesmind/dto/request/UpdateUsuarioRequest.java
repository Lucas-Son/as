package com.fiap.esoa.salesmind.dto.request;

import com.fiap.esoa.salesmind.enums.Funcao;

public record UpdateUsuarioRequest(
        String nome,
        String email,
        Funcao funcao) {
    public boolean hasValidUpdate() {
        return (nome != null && !nome.trim().isEmpty())
                || (email != null && !email.trim().isEmpty())
                || (funcao != null);
    }

    public String getValidationError() {
        if (!hasValidUpdate()) {
            return "Pelo menos um campo deve ser fornecido para atualização";
        }
        return null;
    }
}
