package com.fiap.esoa.salesmind.dto.request;

import com.fiap.esoa.salesmind.enums.Funcao;

public record CreateUsuarioRequest(
        String nome,
        String email,
        String senha,
        Funcao funcao) {
    public boolean isValid() {
        return nome != null && !nome.trim().isEmpty()
                && email != null && !email.trim().isEmpty()
                && senha != null && !senha.trim().isEmpty()
                && funcao != null;
    }

    public String getValidationError() {
        if (nome == null || nome.trim().isEmpty()) {
            return "Nome é obrigatório";
        }
        if (email == null || email.trim().isEmpty()) {
            return "Email é obrigatório";
        }
        if (senha == null || senha.trim().isEmpty()) {
            return "Senha é obrigatória";
        }
        if (funcao == null) {
            return "Funcao deve ser VENDEDOR, GERENTE ou ADMIN";
        }
        return null;
    }
}
