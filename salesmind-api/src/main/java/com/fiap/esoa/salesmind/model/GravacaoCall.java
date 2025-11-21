package com.fiap.esoa.salesmind.model;

import com.fiap.esoa.salesmind.enums.StatusVenda;
import com.fiap.esoa.salesmind.enums.StatusProcessamento;
import java.time.LocalDateTime;

public class GravacaoCall {

    private Long id;
    private Long idUsuario;
    private Long idCliente;
    private String audioUrl;
    private String audioFilename;
    private String transcricao;
    private String resumoIA;
    private StatusVenda statusVenda;
    private StatusProcessamento statusProcessamento;
    private Integer duracaoSegundos;
    private String erroProcessamento;
    private LocalDateTime dataGravacao;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
    private FeedbackIA feedback;

    public GravacaoCall() {
        this.statusVenda = StatusVenda.PENDENTE;
        this.statusProcessamento = StatusProcessamento.UPLOADING;
        this.dataGravacao = LocalDateTime.now();
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
    }

    public GravacaoCall(Long id, Long idUsuario, Long idCliente, String audioUrl, String audioFilename) {
        this();
        this.id = id;
        this.idUsuario = idUsuario;
        this.idCliente = idCliente;
        this.audioUrl = audioUrl;
        this.audioFilename = audioFilename;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Long getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Long idCliente) {
        this.idCliente = idCliente;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
        this.atualizadoEm = LocalDateTime.now();
    }

    public String getAudioFilename() {
        return audioFilename;
    }

    public void setAudioFilename(String audioFilename) {
        this.audioFilename = audioFilename;
    }

    public String getTranscricao() {
        return transcricao;
    }

    public void setTranscricao(String transcricao) {
        this.transcricao = transcricao;
        this.atualizadoEm = LocalDateTime.now();
    }

    public String getResumoIA() {
        return resumoIA;
    }

    public void setResumoIA(String resumoIA) {
        this.resumoIA = resumoIA;
        this.atualizadoEm = LocalDateTime.now();
    }

    public StatusVenda getStatusVenda() {
        return statusVenda;
    }

    public void setStatusVenda(StatusVenda statusVenda) {
        this.statusVenda = statusVenda;
        this.atualizadoEm = LocalDateTime.now();
    }

    public StatusProcessamento getStatusProcessamento() {
        return statusProcessamento;
    }

    public void setStatusProcessamento(StatusProcessamento statusProcessamento) {
        this.statusProcessamento = statusProcessamento;
        this.atualizadoEm = LocalDateTime.now();
    }

    public Integer getDuracaoSegundos() {
        return duracaoSegundos;
    }

    public void setDuracaoSegundos(Integer duracaoSegundos) {
        this.duracaoSegundos = duracaoSegundos;
    }

    public String getErroProcessamento() {
        return erroProcessamento;
    }

    public void setErroProcessamento(String erroProcessamento) {
        this.erroProcessamento = erroProcessamento;
        this.atualizadoEm = LocalDateTime.now();
    }

    public LocalDateTime getDataGravacao() {
        return dataGravacao;
    }

    public void setDataGravacao(LocalDateTime dataGravacao) {
        this.dataGravacao = dataGravacao;
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

    public FeedbackIA getFeedback() {
        return feedback;
    }

    public void setFeedback(FeedbackIA feedback) {
        this.feedback = feedback;
    }
}
