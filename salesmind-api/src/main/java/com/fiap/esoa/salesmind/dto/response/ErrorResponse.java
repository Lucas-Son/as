package com.fiap.esoa.salesmind.dto.response;

import java.time.LocalDateTime;

public record ErrorResponse(String error, Long status, LocalDateTime timestamp) {
    public ErrorResponse(String error, Long status) {
        this(error, status, LocalDateTime.now());
    }
}
