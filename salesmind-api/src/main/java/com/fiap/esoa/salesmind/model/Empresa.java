package com.fiap.esoa.salesmind.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Empresa {

    private Long id;
    private String nomeEmpresa;
    private String cnpj;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
    private List<Usuario> usuarios;
    private List<Cliente> clientes;

    public Empresa() {
        this.usuarios = new ArrayList<>();
        this.clientes = new ArrayList<>();
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
    }

    public Empresa(Long id, String nomeEmpresa, String cnpj) {
        this();
        this.id = id;
        this.nomeEmpresa = nomeEmpresa;
        this.cnpj = cnpj;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomeEmpresa() {
        return nomeEmpresa;
    }

    public void setNomeEmpresa(String nomeEmpresa) {
        this.nomeEmpresa = nomeEmpresa;
        this.atualizadoEm = LocalDateTime.now();
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
        this.atualizadoEm = LocalDateTime.now();
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    public List<Usuario> getUsuarios() {
        return usuarios;
    }

    public void setUsuarios(List<Usuario> usuarios) {
        this.usuarios = usuarios;
    }

    public List<Cliente> getClientes() {
        return clientes;
    }

    public void setClientes(List<Cliente> clientes) {
        this.clientes = clientes;
    }
}
