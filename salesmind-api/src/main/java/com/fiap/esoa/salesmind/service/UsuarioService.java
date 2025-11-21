package com.fiap.esoa.salesmind.service;

import com.fiap.esoa.salesmind.model.Usuario;
import com.fiap.esoa.salesmind.repository.UsuarioRepository;
import com.fiap.esoa.salesmind.repository.GravacaoCallRepository;
import com.fiap.esoa.salesmind.util.PasswordUtil;
import java.util.List;
import java.util.Optional;

public class UsuarioService {

    private final UsuarioRepository repository;
    private final GravacaoCallRepository gravacaoRepository;

    public UsuarioService(UsuarioRepository repository, GravacaoCallRepository gravacaoRepository) {
        this.repository = repository;
        this.gravacaoRepository = gravacaoRepository;
    }

    public Usuario create(Usuario usuario) {
        if (usuario.getSenha() != null && !usuario.getSenha().isEmpty()) {
            usuario.setSenha(PasswordUtil.hashPassword(usuario.getSenha()));
        }
        return repository.save(usuario);
    }

    public Optional<Usuario> findById(Long id) {
        return repository.findById(id);
    }
    
    public Usuario findByEmail(String email) {
        return repository.findByEmail(email).orElse(null);
    }

    public List<Usuario> findAll() {
        return repository.findAll();
    }

    public List<Usuario> findByEmpresa(Long idEmpresa) {
        return repository.findByEmpresa(idEmpresa);
    }

    public long getVendasFechadas(Long idUsuario) {
        return gravacaoRepository.countVendasFechadasByUsuario(idUsuario);
    }

    public long countGravacoesByUsuario(Long idUsuario) {
        return gravacaoRepository.countByUsuario(idUsuario);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
