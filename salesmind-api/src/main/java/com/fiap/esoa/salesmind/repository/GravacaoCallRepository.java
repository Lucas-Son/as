package com.fiap.esoa.salesmind.repository;

import com.fiap.esoa.salesmind.config.DatabaseConfig;
import com.fiap.esoa.salesmind.model.GravacaoCall;
import com.fiap.esoa.salesmind.enums.StatusVenda;
import com.fiap.esoa.salesmind.enums.StatusProcessamento;
import com.fiap.esoa.salesmind.util.TransactionManager;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class GravacaoCallRepository {

    public GravacaoCall save(GravacaoCall gravacao) {
        if (gravacao.getId() == null) {
            return insert(gravacao);
        } else {
            return update(gravacao);
        }
    }

    public GravacaoCall saveWithConnection(Connection conn, GravacaoCall gravacao) throws SQLException {
        if (gravacao.getId() == null) {
            return insertWithConnection(conn, gravacao);
        } else {
            return updateWithConnection(conn, gravacao);
        }
    }

    private GravacaoCall insert(GravacaoCall gravacao) {
        return TransactionManager.executeTransaction(conn -> {
            String sql = "INSERT INTO gravacao_call (id_usuario, id_cliente, audio_filename, audio_url, " +
                    "duracao_segundos, transcricao, resumo_ia, status_venda, status_processamento, " +
                    "erro_processamento, data_gravacao, criado_em, atualizado_em) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                LocalDateTime now = LocalDateTime.now();
                gravacao.setCriadoEm(now);
                gravacao.setAtualizadoEm(now);

                stmt.setLong(1, gravacao.getIdUsuario());
                stmt.setLong(2, gravacao.getIdCliente());
                stmt.setString(3, gravacao.getAudioFilename());
                stmt.setString(4, gravacao.getAudioUrl());
                stmt.setObject(5, gravacao.getDuracaoSegundos(), Types.INTEGER);
                stmt.setString(6, gravacao.getTranscricao());
                stmt.setString(7, gravacao.getResumoIA());
                stmt.setString(8, gravacao.getStatusVenda().name());
                stmt.setString(9, gravacao.getStatusProcessamento().name());
                stmt.setString(10, gravacao.getErroProcessamento());
                stmt.setTimestamp(11, Timestamp.valueOf(gravacao.getDataGravacao()));
                stmt.setTimestamp(12, Timestamp.valueOf(gravacao.getCriadoEm()));
                stmt.setTimestamp(13, Timestamp.valueOf(gravacao.getAtualizadoEm()));

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    gravacao.setId(rs.getLong("id"));
                }

                return gravacao;

            } catch (SQLException e) {
                throw new RuntimeException("Erro ao inserir gravacao_call: " + e.getMessage(), e);
            }
        });
    }

    private GravacaoCall insertWithConnection(Connection conn, GravacaoCall gravacao) throws SQLException {
        String sql = "INSERT INTO gravacao_call (id_usuario, id_cliente, audio_filename, audio_url, " +
                "duracao_segundos, transcricao, resumo_ia, status_venda, status_processamento, " +
                "erro_processamento, data_gravacao, criado_em, atualizado_em) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            LocalDateTime now = LocalDateTime.now();
            gravacao.setCriadoEm(now);
            gravacao.setAtualizadoEm(now);

            stmt.setLong(1, gravacao.getIdUsuario());
            stmt.setLong(2, gravacao.getIdCliente());
            stmt.setString(3, gravacao.getAudioFilename());
            stmt.setString(4, gravacao.getAudioUrl());
            stmt.setObject(5, gravacao.getDuracaoSegundos(), Types.INTEGER);
            stmt.setString(6, gravacao.getTranscricao());
            stmt.setString(7, gravacao.getResumoIA());
            stmt.setString(8, gravacao.getStatusVenda().name());
            stmt.setString(9, gravacao.getStatusProcessamento().name());
            stmt.setString(10, gravacao.getErroProcessamento());
            stmt.setTimestamp(11, Timestamp.valueOf(gravacao.getDataGravacao()));
            stmt.setTimestamp(12, Timestamp.valueOf(gravacao.getCriadoEm()));
            stmt.setTimestamp(13, Timestamp.valueOf(gravacao.getAtualizadoEm()));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                gravacao.setId(rs.getLong("id"));
            }

            return gravacao;

        } catch (SQLException e) {
            throw new SQLException("Erro ao inserir gravacao_call: " + e.getMessage(), e);
        }
    }

    private GravacaoCall update(GravacaoCall gravacao) {
        return TransactionManager.executeTransaction(conn -> {
            String sql = "UPDATE gravacao_call SET id_usuario = ?, id_cliente = ?, audio_filename = ?, " +
                    "audio_url = ?, duracao_segundos = ?, transcricao = ?, resumo_ia = ?, " +
                    "status_venda = ?, status_processamento = ?, erro_processamento = ?, " +
                    "data_gravacao = ?, atualizado_em = ? WHERE id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                gravacao.setAtualizadoEm(LocalDateTime.now());

                stmt.setLong(1, gravacao.getIdUsuario());
                stmt.setLong(2, gravacao.getIdCliente());
                stmt.setString(3, gravacao.getAudioFilename());
                stmt.setString(4, gravacao.getAudioUrl());
                stmt.setObject(5, gravacao.getDuracaoSegundos(), Types.INTEGER);
                stmt.setString(6, gravacao.getTranscricao());
                stmt.setString(7, gravacao.getResumoIA());
                stmt.setString(8, gravacao.getStatusVenda().name());
                stmt.setString(9, gravacao.getStatusProcessamento().name());
                stmt.setString(10, gravacao.getErroProcessamento());
                stmt.setTimestamp(11, Timestamp.valueOf(gravacao.getDataGravacao()));
                stmt.setTimestamp(12, Timestamp.valueOf(gravacao.getAtualizadoEm()));
                stmt.setLong(13, gravacao.getId());

                stmt.executeUpdate();
                return gravacao;

            } catch (SQLException e) {
                throw new RuntimeException("Erro ao atualizar gravacao_call: " + e.getMessage(), e);
            }
        });
    }

    private GravacaoCall updateWithConnection(Connection conn, GravacaoCall gravacao) throws SQLException {
        String sql = "UPDATE gravacao_call SET id_usuario = ?, id_cliente = ?, audio_filename = ?, " +
                "audio_url = ?, duracao_segundos = ?, transcricao = ?, resumo_ia = ?, " +
                "status_venda = ?, status_processamento = ?, erro_processamento = ?, " +
                "data_gravacao = ?, atualizado_em = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            gravacao.setAtualizadoEm(LocalDateTime.now());

            stmt.setLong(1, gravacao.getIdUsuario());
            stmt.setLong(2, gravacao.getIdCliente());
            stmt.setString(3, gravacao.getAudioFilename());
            stmt.setString(4, gravacao.getAudioUrl());
            stmt.setObject(5, gravacao.getDuracaoSegundos(), Types.INTEGER);
            stmt.setString(6, gravacao.getTranscricao());
            stmt.setString(7, gravacao.getResumoIA());
            stmt.setString(8, gravacao.getStatusVenda().name());
            stmt.setString(9, gravacao.getStatusProcessamento().name());
            stmt.setString(10, gravacao.getErroProcessamento());
            stmt.setTimestamp(11, Timestamp.valueOf(gravacao.getDataGravacao()));
            stmt.setTimestamp(12, Timestamp.valueOf(gravacao.getAtualizadoEm()));
            stmt.setLong(13, gravacao.getId());

            stmt.executeUpdate();
            return gravacao;

        } catch (SQLException e) {
            throw new SQLException("Erro ao atualizar gravacao_call: " + e.getMessage(), e);
        }
    }

    public Optional<GravacaoCall> findById(Long id) {
        String sql = "SELECT * FROM gravacao_call WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToGravacaoCall(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar gravação por id: " + e.getMessage(), e);
        }
    }

    public List<GravacaoCall> findAll() {
        String sql = "SELECT * FROM gravacao_call ORDER BY id";
        List<GravacaoCall> gravacoes = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                gravacoes.add(mapResultSetToGravacaoCall(rs));
            }
            return gravacoes;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar todas as gravações: " + e.getMessage(), e);
        }
    }

    public List<GravacaoCall> findByUsuario(Long idUsuario) {
        String sql = "SELECT * FROM gravacao_call WHERE id_usuario = ? ORDER BY id";
        List<GravacaoCall> gravacoes = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, idUsuario);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                gravacoes.add(mapResultSetToGravacaoCall(rs));
            }
            return gravacoes;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar gravações por usuário: " + e.getMessage(), e);
        }
    }

    public List<GravacaoCall> findByCliente(Long idCliente) {
        String sql = "SELECT * FROM gravacao_call WHERE id_cliente = ? ORDER BY id";
        List<GravacaoCall> gravacoes = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, idCliente);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                gravacoes.add(mapResultSetToGravacaoCall(rs));
            }
            return gravacoes;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar gravações por cliente: " + e.getMessage(), e);
        }
    }

    public List<GravacaoCall> findByStatusVenda(StatusVenda status) {
        String sql = "SELECT * FROM gravacao_call WHERE status_venda = ? ORDER BY id";
        List<GravacaoCall> gravacoes = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                gravacoes.add(mapResultSetToGravacaoCall(rs));
            }
            return gravacoes;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar gravações por status de venda: " + e.getMessage(), e);
        }
    }

    public List<GravacaoCall> findByStatusProcessamento(StatusProcessamento status) {
        String sql = "SELECT * FROM gravacao_call WHERE status_processamento = ? ORDER BY id";
        List<GravacaoCall> gravacoes = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                gravacoes.add(mapResultSetToGravacaoCall(rs));
            }
            return gravacoes;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar gravações por status de processamento: " + e.getMessage(), e);
        }
    }

    public long countVendasFechadasByUsuario(Long idUsuario) {
        String sql = "SELECT COUNT(*) FROM gravacao_call WHERE id_usuario = ? AND status_venda = 'FECHADO'";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, idUsuario);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar vendas fechadas por usuário: " + e.getMessage(), e);
        }
    }

    public long countVendasFechadasByCliente(Long idCliente) {
        String sql = "SELECT COUNT(*) FROM gravacao_call WHERE id_cliente = ? AND status_venda = 'FECHADO'";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, idCliente);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar vendas fechadas por cliente: " + e.getMessage(), e);
        }
    }

    public void deleteById(Long id) {
        TransactionManager.executeTransactionVoid(conn -> {
            String sql = "DELETE FROM gravacao_call WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, id);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao excluir gravação: " + e.getMessage(), e);
            }
        });
    }

    public boolean existsById(Long id) {
        String sql = "SELECT EXISTS(SELECT 1 FROM gravacao_call WHERE id = ?)";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBoolean(1);
            }
            return false;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao verificar se gravação existe: " + e.getMessage(), e);
        }
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM gravacao_call";

        try (Connection conn = DatabaseConfig.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar gravações: " + e.getMessage(), e);
        }
    }

    public long countVendasFechadas() {
        String sql = "SELECT COUNT(*) FROM gravacao_call WHERE status_venda = 'FECHADA'";

        try (Connection conn = DatabaseConfig.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar vendas fechadas: " + e.getMessage(), e);
        }
    }

    public long countByEmpresa(Long idEmpresa) {
        String sql = "SELECT COUNT(*) FROM gravacao_call g " +
                "INNER JOIN usuario u ON g.id_usuario = u.id " +
                "WHERE u.id_empresa = ?";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, idEmpresa);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar gravações por empresa: " + e.getMessage(), e);
        }
    }

    public long countVendasFechadasByEmpresa(Long idEmpresa) {
        String sql = "SELECT COUNT(*) FROM gravacao_call g " +
                "INNER JOIN usuario u ON g.id_usuario = u.id " +
                "WHERE u.id_empresa = ? AND g.status_venda = 'FECHADA'";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, idEmpresa);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar vendas fechadas por empresa: " + e.getMessage(), e);
        }
    }

    public long countByUsuario(Long idUsuario) {
        String sql = "SELECT COUNT(*) FROM gravacao_call WHERE id_usuario = ?";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, idUsuario);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar gravações por usuário: " + e.getMessage(), e);
        }
    }

    public long countByCliente(Long idCliente) {
        String sql = "SELECT COUNT(*) FROM gravacao_call WHERE id_cliente = ?";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, idCliente);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar gravações por cliente: " + e.getMessage(), e);
        }
    }

    private GravacaoCall mapResultSetToGravacaoCall(ResultSet rs) throws SQLException {
        GravacaoCall gravacao = new GravacaoCall();
        gravacao.setId(rs.getLong("id"));
        gravacao.setIdUsuario(rs.getLong("id_usuario"));
        gravacao.setIdCliente(rs.getLong("id_cliente"));
        gravacao.setAudioFilename(rs.getString("audio_filename"));
        gravacao.setAudioUrl(rs.getString("audio_url"));

        Integer duracao = (Integer) rs.getObject("duracao_segundos");
        gravacao.setDuracaoSegundos(duracao);

        gravacao.setTranscricao(rs.getString("transcricao"));
        gravacao.setResumoIA(rs.getString("resumo_ia"));
        gravacao.setStatusVenda(StatusVenda.valueOf(rs.getString("status_venda")));
        gravacao.setStatusProcessamento(StatusProcessamento.valueOf(rs.getString("status_processamento")));
        gravacao.setErroProcessamento(rs.getString("erro_processamento"));
        gravacao.setDataGravacao(rs.getTimestamp("data_gravacao").toLocalDateTime());
        gravacao.setCriadoEm(rs.getTimestamp("criado_em").toLocalDateTime());
        gravacao.setAtualizadoEm(rs.getTimestamp("atualizado_em").toLocalDateTime());

        Long gravacaoId = gravacao.getId();
        if (gravacaoId != null) {
            FeedbackIARepository feedbackRepo = new FeedbackIARepository();
            feedbackRepo.findByGravacaoId(gravacaoId).ifPresent(gravacao::setFeedback);
        }

        return gravacao;
    }
}
