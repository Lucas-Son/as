package com.fiap.esoa.salesmind.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String resourceType, Long id) {
        super(String.format("%s with id %d not found", resourceType, id));
    }

    public NotFoundException(String resourceType, String identifier) {
        super(String.format("%s with identifier '%s' not found", resourceType, identifier));
    }
}
