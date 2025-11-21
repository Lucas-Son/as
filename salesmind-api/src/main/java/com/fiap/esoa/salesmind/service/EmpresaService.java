package com.fiap.esoa.salesmind.service;

import com.fiap.esoa.salesmind.model.Empresa;
import com.fiap.esoa.salesmind.repository.EmpresaRepository;
import com.fiap.esoa.salesmind.repository.UsuarioRepository;
import com.fiap.esoa.salesmind.repository.ClienteRepository;
import com.fiap.esoa.salesmind.repository.GravacaoCallRepository;
import java.util.List;
import java.util.Optional;

public class EmpresaService {

    private final EmpresaRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final GravacaoCallRepository gravacaoRepository;

    public EmpresaService(EmpresaRepository repository, UsuarioRepository usuarioRepository, 
                         ClienteRepository clienteRepository, GravacaoCallRepository gravacaoRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.gravacaoRepository = gravacaoRepository;
    }

    public Empresa create(Empresa empresa) {
        return repository.save(empresa);
    }

    public Optional<Empresa> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<Empresa> findByCnpj(String cnpj) {
        return repository.findByCnpj(cnpj);
    }

    public List<Empresa> findAll() {
        return repository.findAll();
    }

    public boolean hasDependencies(Long empresaId) {
        long usuarios = usuarioRepository.countByEmpresa(empresaId);
        long clientes = clienteRepository.countByEmpresa(empresaId);
        long gravacoes = gravacaoRepository.countByEmpresa(empresaId);
        
        return usuarios > 0 || clientes > 0 || gravacoes > 0;
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
