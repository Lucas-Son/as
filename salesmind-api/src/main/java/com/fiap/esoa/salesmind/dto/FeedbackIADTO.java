package com.fiap.esoa.salesmind.dto;

import com.fiap.esoa.salesmind.enums.CategoriaAmbiental;
import com.fiap.esoa.salesmind.model.FeedbackIA;
import java.time.LocalDateTime;
import java.util.List;

public record FeedbackIADTO(
        Long id,
        Long idGravacao,
        Long idEmpresa,
        List<String> pontosFortes,
        List<String> pontosFracos,
        List<String> sugestoes,
        Integer sentimentScore,
        Integer probabilidadeFechamento,
        CategoriaAmbiental categoriaAmbiental,
        Integer qualidadeAtendimento,
        Integer aderenciaScript,
        Integer gestaoObjecoes,
        List<String> objecoesIdentificadas,
        List<String> momentosChave,
        LocalDateTime criadoEm) {
    public static FeedbackIADTO fromEntity(FeedbackIA feedback) {
        if (feedback == null) {
            return null;
        }

        return new FeedbackIADTO(
                feedback.getId(),
                feedback.getIdGravacao(),
                feedback.getIdEmpresa(),
                feedback.getPontosFortes(),
                feedback.getPontosFracos(),
                feedback.getSugestoes(),
                feedback.getSentimentScore(),
                feedback.getProbabilidadeFechamento(),
                feedback.getCategoriaAmbientalCalculada(),
                feedback.getQualidadeAtendimento(),
                feedback.getAderenciaScript(),
                feedback.getGestaoObjecoes(),
                feedback.getObjecoesIdentificadas(),
                feedback.getMomentosChave(),
                feedback.getCriadoEm());
    }
}
