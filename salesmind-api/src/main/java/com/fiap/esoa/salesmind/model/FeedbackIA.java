package com.fiap.esoa.salesmind.model;

import com.fiap.esoa.salesmind.enums.CategoriaAmbiental;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FeedbackIA {

    private Long id;
    private Long idGravacao;
    private Long idEmpresa;
    private List<String> pontosFortes;
    private List<String> pontosFracos;
    private List<String> sugestoes;
    private Integer sentimentScore;
    private Integer probabilidadeFechamento;
    private CategoriaAmbiental categoriaAmbiental;
    private Integer qualidadeAtendimento;
    private Integer aderenciaScript;
    private Integer gestaoObjecoes;
    private List<String> objecoesIdentificadas;
    private List<String> momentosChave;

    private LocalDateTime criadoEm;

    public FeedbackIA() {
        this.pontosFortes = new ArrayList<>();
        this.pontosFracos = new ArrayList<>();
        this.sugestoes = new ArrayList<>();
        this.objecoesIdentificadas = new ArrayList<>();
        this.momentosChave = new ArrayList<>();
        this.criadoEm = LocalDateTime.now();
    }

    public FeedbackIA(Long id, Long idGravacao) {
        this();
        this.id = id;
        this.idGravacao = idGravacao;
    }

    public FeedbackIA(Long id, Long idGravacao, Long idEmpresa) {
        this();
        this.id = id;
        this.idGravacao = idGravacao;
        this.idEmpresa = idEmpresa;
    }

    public CategoriaAmbiental getCategoriaAmbientalCalculada() {
        if (categoriaAmbiental != null) {
            return categoriaAmbiental;
        }

        if (sentimentScore == null) {
            return null;
        }

        if (sentimentScore >= 70) {
            return CategoriaAmbiental.POSITIVO;
        } else if (sentimentScore >= 40) {
            return CategoriaAmbiental.NEUTRO;
        } else {
            return CategoriaAmbiental.NEGATIVO;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdGravacao() {
        return idGravacao;
    }

    public void setIdGravacao(Long idGravacao) {
        this.idGravacao = idGravacao;
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public List<String> getPontosFortes() {
        return pontosFortes;
    }

    public void setPontosFortes(List<String> pontosFortes) {
        this.pontosFortes = pontosFortes;
    }

    public List<String> getPontosFracos() {
        return pontosFracos;
    }

    public void setPontosFracos(List<String> pontosFracos) {
        this.pontosFracos = pontosFracos;
    }

    public List<String> getSugestoes() {
        return sugestoes;
    }

    public void setSugestoes(List<String> sugestoes) {
        this.sugestoes = sugestoes;
    }

    public Integer getSentimentScore() {
        return sentimentScore;
    }

    public void setSentimentScore(Integer sentimentScore) {
        this.sentimentScore = sentimentScore;
    }

    public Integer getProbabilidadeFechamento() {
        return probabilidadeFechamento;
    }

    public void setProbabilidadeFechamento(Integer probabilidadeFechamento) {
        this.probabilidadeFechamento = probabilidadeFechamento;
    }

    public CategoriaAmbiental getCategoriaAmbiental() {
        return categoriaAmbiental;
    }

    public void setCategoriaAmbiental(CategoriaAmbiental categoriaAmbiental) {
        this.categoriaAmbiental = categoriaAmbiental;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Integer getQualidadeAtendimento() {
        return qualidadeAtendimento;
    }

    public void setQualidadeAtendimento(Integer qualidadeAtendimento) {
        this.qualidadeAtendimento = qualidadeAtendimento;
    }

    public Integer getAderenciaScript() {
        return aderenciaScript;
    }

    public void setAderenciaScript(Integer aderenciaScript) {
        this.aderenciaScript = aderenciaScript;
    }

    public Integer getGestaoObjecoes() {
        return gestaoObjecoes;
    }

    public void setGestaoObjecoes(Integer gestaoObjecoes) {
        this.gestaoObjecoes = gestaoObjecoes;
    }

    public List<String> getObjecoesIdentificadas() {
        return objecoesIdentificadas;
    }

    public void setObjecoesIdentificadas(List<String> objecoesIdentificadas) {
        this.objecoesIdentificadas = objecoesIdentificadas;
    }

    public List<String> getMomentosChave() {
        return momentosChave;
    }

    public void setMomentosChave(List<String> momentosChave) {
        this.momentosChave = momentosChave;
    }
}
