package com.fiap.esoa.salesmind.dto;

import com.fiap.esoa.salesmind.enums.Funcao;
import com.fiap.esoa.salesmind.model.Usuario;
import java.time.LocalDateTime;

public record UsuarioDTO(
        Long id,
        Long idEmpresa,
        String nome,
        String email,
        Funcao funcao,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {
    
    public static UsuarioDTO fromEntity(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        
        return new UsuarioDTO(
                usuario.getId(),
                usuario.getIdEmpresa(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getFuncao(),
                usuario.getCriadoEm(),
                usuario.getAtualizadoEm());
    }
}
