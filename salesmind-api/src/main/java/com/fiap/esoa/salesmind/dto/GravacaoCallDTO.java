package com.fiap.esoa.salesmind.dto;

import com.fiap.esoa.salesmind.enums.StatusProcessamento;
import com.fiap.esoa.salesmind.enums.StatusVenda;
import com.fiap.esoa.salesmind.model.GravacaoCall;
import java.time.LocalDateTime;

public record GravacaoCallDTO(
        Long id,
        Long idUsuario,
        Long idCliente,
        String audioUrl,
        String audioFilename,
        String transcricao,
        String resumoIA,
        StatusVenda statusVenda,
        StatusProcessamento statusProcessamento,
        Integer duracaoSegundos,
        String erroProcessamento,
        LocalDateTime dataGravacao,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm,
        FeedbackIADTO feedback) {
    public static GravacaoCallDTO fromEntity(GravacaoCall gravacao) {
        if (gravacao == null) {
            return null;
        }

        return new GravacaoCallDTO(
                gravacao.getId(),
                gravacao.getIdUsuario(),
                gravacao.getIdCliente(),
                gravacao.getAudioUrl(),
                gravacao.getAudioFilename(),
                gravacao.getTranscricao(),
                gravacao.getResumoIA(),
                gravacao.getStatusVenda(),
                gravacao.getStatusProcessamento(),
                gravacao.getDuracaoSegundos(),
                gravacao.getErroProcessamento(),
                gravacao.getDataGravacao(),
                gravacao.getCriadoEm(),
                gravacao.getAtualizadoEm(),
                FeedbackIADTO.fromEntity(gravacao.getFeedback()));
    }
}
