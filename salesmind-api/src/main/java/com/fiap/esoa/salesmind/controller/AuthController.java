package com.fiap.esoa.salesmind.controller;

import com.fiap.esoa.salesmind.dto.request.LoginRequest;
import com.fiap.esoa.salesmind.dto.request.RefreshTokenRequest;
import com.fiap.esoa.salesmind.dto.response.LoginResponse;
import com.fiap.esoa.salesmind.model.Usuario;
import com.fiap.esoa.salesmind.service.UsuarioService;
import com.fiap.esoa.salesmind.util.JsonUtil;
import com.fiap.esoa.salesmind.util.PasswordUtil;
import com.sun.net.httpserver.HttpExchange;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Date;

/**
 * Controller para endpoints de autenticação.
 * Gerencia operações de login e atualização de token.
 */
public class AuthController extends BaseController {
    
    private final SecretKey jwtKey;
    private final UsuarioService usuarioService;
    
    private static final long ACCESS_TOKEN_EXPIRY_MS = 3600000L;  // 1 hora
    private static final long REFRESH_TOKEN_EXPIRY_MS = 2592000000L;  // 30 dias

    public AuthController(SecretKey jwtKey, UsuarioService usuarioService) {
        this.jwtKey = jwtKey;
        this.usuarioService = usuarioService;
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("POST".equals(method) && path.endsWith("/login")) {
            handleLogin(exchange);
        } else if ("POST".equals(method) && path.endsWith("/refresh")) {
            handleRefresh(exchange);
        } else {
            JsonUtil.sendErrorResponse(exchange, 404, "Endpoint não encontrado");
        }
    }

    /**
     * POST /api/auth/login
     * Autentica usuário e retorna tokens JWT
     */
    private void handleLogin(HttpExchange exchange) throws IOException {
        try {
            String body = getRequestBody(exchange);
            LoginRequest request = JsonUtil.fromJson(body, LoginRequest.class);

            if (request.email() == null || request.senha() == null) {
                JsonUtil.sendErrorResponse(exchange, 400, "Email e senha são obrigatórios");
                return;
            }

            Usuario usuario = usuarioService.findByEmail(request.email());
            if (usuario == null) {
                JsonUtil.sendErrorResponse(exchange, 401, "Email ou senha inválidos");
                return;
            }

            if (!PasswordUtil.checkPassword(request.senha(), usuario.getSenha())) {
                JsonUtil.sendErrorResponse(exchange, 401, "Email ou senha inválidos");
                return;
            }

            String accessToken = generateAccessToken(usuario);
            String refreshToken = generateRefreshToken(usuario);

            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getFuncao() != null ? usuario.getFuncao().toString() : null,
                usuario.getIdEmpresa()
            );

            LoginResponse response = new LoginResponse(
                accessToken,
                refreshToken,
                ACCESS_TOKEN_EXPIRY_MS / 1000,
                userInfo
            );

            JsonUtil.sendJsonResponse(exchange, 200, response);

        } catch (Exception e) {
            System.err.println("Erro durante login: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendErrorResponse(exchange, 500, "Erro interno do servidor");
        }
    }

    /**
     * POST /api/auth/refresh
     * Valida token de atualização e emite novo token de acesso
     */
    private void handleRefresh(HttpExchange exchange) throws IOException {
        try {
            String body = getRequestBody(exchange);
            RefreshTokenRequest request = JsonUtil.fromJson(body, RefreshTokenRequest.class);

            if (request.refreshToken() == null || request.refreshToken().isEmpty()) {
                JsonUtil.sendErrorResponse(exchange, 400, "Token de atualização é obrigatório");
                return;
            }

            Jws<Claims> claims;
            try {
                claims = Jwts.parser()
                    .verifyWith(jwtKey)
                    .build()
                    .parseSignedClaims(request.refreshToken());
            } catch (JwtException e) {
                JsonUtil.sendErrorResponse(exchange, 401, "Token de atualização inválido ou expirado");
                return;
            }

            String tokenType = (String) claims.getPayload().get("type");
            if (!"refresh".equals(tokenType)) {
                JsonUtil.sendErrorResponse(exchange, 401, "Tipo de token inválido");
                return;
            }

            String email = claims.getPayload().getSubject();
            Usuario usuario = usuarioService.findByEmail(email);
            if (usuario == null) {
                JsonUtil.sendErrorResponse(exchange, 401, "Usuário não encontrado");
                return;
            }

            String newAccessToken = generateAccessToken(usuario);

            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getFuncao() != null ? usuario.getFuncao().toString() : null,
                usuario.getIdEmpresa()
            );

            LoginResponse response = new LoginResponse(
                newAccessToken,
                request.refreshToken(),
                ACCESS_TOKEN_EXPIRY_MS / 1000,
                userInfo
            );

            JsonUtil.sendJsonResponse(exchange, 200, response);

        } catch (Exception e) {
            System.err.println("Erro durante atualização do token: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendErrorResponse(exchange, 500, "Erro interno do servidor");
        }
    }

    /**
     * Gera token de acesso
     */
    private String generateAccessToken(Usuario usuario) {
        return Jwts.builder()
            .subject(usuario.getEmail())
            .claim("empresaId", usuario.getIdEmpresa().longValue())
            .claim("userId", usuario.getId().longValue())
            .claim("role", usuario.getFuncao() != null ? usuario.getFuncao().toString() : null)
            .claim("type", "access")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY_MS))
            .signWith(jwtKey)
            .compact();
    }

    /**
     * Gerar token de atualização
     */
    private String generateRefreshToken(Usuario usuario) {
        return Jwts.builder()
            .subject(usuario.getEmail())
            .claim("userId", usuario.getId().longValue())
            .claim("type", "refresh")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRY_MS))
            .signWith(jwtKey)
            .compact();
    }
}
