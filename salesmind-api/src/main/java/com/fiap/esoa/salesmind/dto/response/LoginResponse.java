package com.fiap.esoa.salesmind.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        UserInfo user) {
    public LoginResponse(String accessToken, String refreshToken, Long expiresIn, UserInfo user) {
        this(accessToken, refreshToken, "Bearer", expiresIn, user);
    }

    public record UserInfo(
            Long id,
            String nome,
            String email,
            String funcao,
            Long empresaId) {
    }
}
