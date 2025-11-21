package com.fiap.esoa.salesmind.service;

import com.fiap.esoa.salesmind.model.Cliente;
import com.fiap.esoa.salesmind.repository.ClienteRepository;
import com.fiap.esoa.salesmind.repository.GravacaoCallRepository;
import java.util.List;
import java.util.Optional;

public class ClienteService {

    private final ClienteRepository repository;
    private final GravacaoCallRepository gravacaoRepository;

    public ClienteService(ClienteRepository repository, GravacaoCallRepository gravacaoRepository) {
        this.repository = repository;
        this.gravacaoRepository = gravacaoRepository;
    }

    public Cliente create(Cliente cliente) {
        if (cliente.getCpfCnpj() != null && !cliente.getCpfCnpj().trim().isEmpty()) {
            Optional<Cliente> existing = repository.findByCpfCnpj(cliente.getCpfCnpj());
            if (existing.isPresent() && !existing.get().getId().equals(cliente.getId())) {
                throw new IllegalArgumentException("CPF/CNPJ já cadastrado");
            }
        }
        
        if (cliente.getEmail() != null && !cliente.getEmail().trim().isEmpty()) {
            Optional<Cliente> existing = repository.findByEmail(cliente.getEmail());
            if (existing.isPresent() && !existing.get().getId().equals(cliente.getId())) {
                throw new IllegalArgumentException("Email já cadastrado");
            }
        }
        
        if (cliente.getTelefone() != null && !cliente.getTelefone().trim().isEmpty()) {
            Optional<Cliente> existing = repository.findByTelefone(cliente.getTelefone());
            if (existing.isPresent() && !existing.get().getId().equals(cliente.getId())) {
                throw new IllegalArgumentException("Telefone já cadastrado");
            }
        }
        
        return repository.save(cliente);
    }

    public Optional<Cliente> findById(Long id) {
        return repository.findById(id);
    }
    
    public List<Cliente> findByEmpresa(Long idEmpresa) {
        return repository.findByEmpresa(idEmpresa);
    }

    public List<Cliente> findAll() {
        return repository.findAll();
    }

    public long countGravacoesByCliente(Long idCliente) {
        return gravacaoRepository.countByCliente(idCliente);
    }
    
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
