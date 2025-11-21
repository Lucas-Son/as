package com.fiap.esoa.salesmind.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Cliente {

    private Long id;
    private Long idEmpresa;
    private String nome;
    private String cpfCnpj;
    private String telefone;
    private String email;
    private String segmento;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
    private List<GravacaoCall> gravacoes;

    public Cliente() {
        this.gravacoes = new ArrayList<>();
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
    }

    public Cliente(Long id, Long idEmpresa, String nome, String cpfCnpj, String telefone, String email,
            String segmento) {
        this();
        this.id = id;
        this.idEmpresa = idEmpresa;
        this.nome = nome;
        this.cpfCnpj = cpfCnpj;
        this.telefone = telefone;
        this.email = email;
        this.segmento = segmento;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
        this.atualizadoEm = LocalDateTime.now();
    }

    public String getCpfCnpj() {
        return cpfCnpj;
    }

    public void setCpfCnpj(String cpfCnpj) {
        this.cpfCnpj = cpfCnpj;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
        this.atualizadoEm = LocalDateTime.now();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
        this.atualizadoEm = LocalDateTime.now();
    }

    public String getSegmento() {
        return segmento;
    }

    public void setSegmento(String segmento) {
        this.segmento = segmento;
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

    public List<GravacaoCall> getGravacoes() {
        return gravacoes;
    }

    public void setGravacoes(List<GravacaoCall> gravacoes) {
        this.gravacoes = gravacoes;
    }
}
