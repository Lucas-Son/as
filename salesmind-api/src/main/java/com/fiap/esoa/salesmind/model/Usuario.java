package com.fiap.esoa.salesmind.model;

import com.fiap.esoa.salesmind.enums.Funcao;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Usuario {

    private Long id;
    private Long idEmpresa;
    private String nome;
    private String email;
    private String senha;
    private Funcao funcao;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
    private List<GravacaoCall> gravacoes;

    public Usuario() {
        this.gravacoes = new ArrayList<>();
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
    }

    public Usuario(Long id, Long idEmpresa, String nome, String email, Funcao funcao) {
        this();
        this.id = id;
        this.idEmpresa = idEmpresa;
        this.nome = nome;
        this.email = email;
        this.funcao = funcao;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
        this.atualizadoEm = LocalDateTime.now();
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public Funcao getFuncao() {
        return funcao;
    }

    public void setFuncao(Funcao funcao) {
        this.funcao = funcao;
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
