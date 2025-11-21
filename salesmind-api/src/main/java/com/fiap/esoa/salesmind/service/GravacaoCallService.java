package com.fiap.esoa.salesmind.service;

import com.fiap.esoa.salesmind.exception.NotFoundException;
import com.fiap.esoa.salesmind.model.FeedbackIA;
import com.fiap.esoa.salesmind.model.GravacaoCall;
import com.fiap.esoa.salesmind.model.Usuario;
import com.fiap.esoa.salesmind.enums.StatusProcessamento;
import com.fiap.esoa.salesmind.enums.StatusVenda;
import com.fiap.esoa.salesmind.repository.GravacaoCallRepository;
import com.fiap.esoa.salesmind.repository.UsuarioRepository;
import com.fiap.esoa.salesmind.util.TransactionManager;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class GravacaoCallService {

    private final GravacaoCallRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final FeedbackIAService feedbackService;
    private final GeminiService geminiService;

    public GravacaoCallService(GravacaoCallRepository repository,
            UsuarioRepository usuarioRepository,
            FeedbackIAService feedbackService,
            GeminiService geminiService) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.feedbackService = feedbackService;
        this.geminiService = geminiService;
    }

    public GravacaoCall save(GravacaoCall gravacao) {
        return repository.save(gravacao);
    }

    public Optional<GravacaoCall> findById(Long id) {
        return repository.findById(id);
    }
    
    public List<GravacaoCall> findByUsuario(Long idUsuario) {
        return repository.findByUsuario(idUsuario);
    }

    public List<GravacaoCall> findAll() {
        return repository.findAll();
    }

    public CompletableFuture<Void> processAudioAsync(Long gravacaoId) {
        return CompletableFuture.runAsync(() -> {
            try {
                GravacaoCall gravacao = repository.findById(gravacaoId)
                        .orElseThrow(() -> new NotFoundException("Gravacao not found: " + gravacaoId));

                gravacao.setStatusProcessamento(StatusProcessamento.PROCESSANDO);
                repository.save(gravacao);

                String transcription = geminiService.transcribeAudio(gravacao.getAudioUrl());
                GeminiService.GeminiAnalysisResult analysis = geminiService.analyzeCall(transcription);

                final String finalTranscription = transcription;
                final GeminiService.GeminiAnalysisResult finalAnalysis = analysis;
                final Long finalGravacaoId = gravacaoId;

                TransactionManager.executeTransactionVoid(conn -> {
                    try {
                        GravacaoCall txGravacao = repository.findById(finalGravacaoId)
                                .orElseThrow(() -> new RuntimeException("Gravacao not found in transaction"));

                        Usuario usuario = usuarioRepository.findById(txGravacao.getIdUsuario())
                                .orElseThrow(() -> new RuntimeException("Usuario not found"));
                        Long empresaId = usuario.getIdEmpresa();

                        txGravacao.setTranscricao(finalTranscription);
                        repository.saveWithConnection(conn, txGravacao);

                        FeedbackIA feedback = new FeedbackIA();
                        feedback.setIdGravacao(finalGravacaoId);
                        feedback.setIdEmpresa(empresaId);
                        feedback.setPontosFortes(Arrays.asList(finalAnalysis.pontosFortes));
                        feedback.setPontosFracos(Arrays.asList(finalAnalysis.pontosFracos));
                        feedback.setSugestoes(Arrays.asList(finalAnalysis.sugestoes));
                        feedback.setSentimentScore(finalAnalysis.sentimentScore);
                        feedback.setProbabilidadeFechamento(finalAnalysis.probabilidadeFechamento);
                        feedback.setCategoriaAmbiental(finalAnalysis.categoriaAmbiental);
                        feedback.setQualidadeAtendimento(finalAnalysis.qualidadeAtendimento);
                        feedback.setAderenciaScript(finalAnalysis.aderenciaScript);
                        feedback.setGestaoObjecoes(finalAnalysis.gestaoObjecoes);
                        feedback.setObjecoesIdentificadas(Arrays.asList(finalAnalysis.objecoesIdentificadas));
                        feedback.setMomentosChave(Arrays.asList(finalAnalysis.momentosChave));

                        FeedbackIA savedFeedback = feedbackService.saveWithConnection(conn, feedback);

                        txGravacao.setResumoIA(finalAnalysis.resumo);
                        txGravacao.setFeedback(savedFeedback);
                        txGravacao.setStatusProcessamento(StatusProcessamento.CONCLUIDO);

                        if (finalAnalysis.probabilidadeFechamento >= 70) {
                            txGravacao.setStatusVenda(StatusVenda.QUALIFICADO);
                        } else if (finalAnalysis.probabilidadeFechamento >= 40) {
                            txGravacao.setStatusVenda(StatusVenda.PROPOSTA_ENVIADA);
                        } else {
                            txGravacao.setStatusVenda(StatusVenda.PENDENTE);
                        }

                        repository.saveWithConnection(conn, txGravacao);

                    } catch (Exception txError) {
                        throw new RuntimeException("Falha na transação durante operações de banco", txError);
                    }
                });

            } catch (Exception e) {
                try {
                    GravacaoCall errorGravacao = repository.findById(gravacaoId).orElse(null);
                    if (errorGravacao != null) {
                        errorGravacao.setStatusProcessamento(StatusProcessamento.ERRO);
                        errorGravacao.setErroProcessamento(e.getMessage());
                        repository.save(errorGravacao);
                    }
                } catch (Exception saveError) {
                    System.err.println("Falha ao salvar status de erro: " + saveError.getMessage());
                }
            }
        });
    }
}
