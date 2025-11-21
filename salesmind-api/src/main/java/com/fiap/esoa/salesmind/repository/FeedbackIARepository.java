package com.fiap.esoa.salesmind.repository;

import com.fiap.esoa.salesmind.config.DatabaseConfig;
import com.fiap.esoa.salesmind.enums.CategoriaAmbiental;
import com.fiap.esoa.salesmind.model.FeedbackIA;
import com.fiap.esoa.salesmind.util.TransactionManager;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class FeedbackIARepository {

    public FeedbackIA save(FeedbackIA feedback) {
        if (feedback.getId() == null) {
            return insert(feedback);
        } else {
            return update(feedback);
        }
    }

    public FeedbackIA saveWithConnection(Connection conn, FeedbackIA feedback) throws SQLException {
        if (feedback.getId() == null) {
            return insertWithConnection(conn, feedback);
        } else {
            return updateWithConnection(conn, feedback);
        }
    }

    private FeedbackIA insert(FeedbackIA feedback) {
        return TransactionManager.executeTransaction(conn -> {
            String sql = "INSERT INTO feedback_ia (id_gravacao, id_empresa, pontos_fortes, pontos_fracos, sugestoes, " +
                    "sentiment_score, probabilidade_fechamento, categoria_ambiental, " +
                    "qualidade_atendimento, aderencia_script, gestao_objecoes, " +
                    "objecoes_identificadas, momentos_chave, criado_em) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                LocalDateTime now = LocalDateTime.now();
                feedback.setCriadoEm(now);

                stmt.setLong(1, feedback.getIdGravacao());
                stmt.setLong(2, feedback.getIdEmpresa());

                stmt.setLong(2, feedback.getIdEmpresa());

                String[] pontosFortes = feedback.getPontosFortes().toArray(new String[0]);
                String[] pontosFracos = feedback.getPontosFracos().toArray(new String[0]);
                String[] sugestoes = feedback.getSugestoes().toArray(new String[0]);
                String[] objecoes = feedback.getObjecoesIdentificadas().toArray(new String[0]);
                String[] momentos = feedback.getMomentosChave().toArray(new String[0]);

                Array pontosFortesSql = conn.createArrayOf("TEXT", pontosFortes);
                Array pontosFracosSql = conn.createArrayOf("TEXT", pontosFracos);
                Array sugestoesSql = conn.createArrayOf("TEXT", sugestoes);
                Array objecoesSql = conn.createArrayOf("TEXT", objecoes);
                Array momentosSql = conn.createArrayOf("TEXT", momentos);

                stmt.setArray(3, pontosFortesSql);
                stmt.setArray(4, pontosFracosSql);
                stmt.setArray(5, sugestoesSql);
                stmt.setObject(6, feedback.getSentimentScore(), Types.INTEGER);
                stmt.setObject(7, feedback.getProbabilidadeFechamento(), Types.INTEGER);
                stmt.setString(8, feedback.getCategoriaAmbiental().name());
                stmt.setObject(9, feedback.getQualidadeAtendimento(), Types.INTEGER);
                stmt.setObject(10, feedback.getAderenciaScript(), Types.INTEGER);
                stmt.setObject(11, feedback.getGestaoObjecoes(), Types.INTEGER);
                stmt.setArray(12, objecoesSql);
                stmt.setArray(13, momentosSql);
                stmt.setTimestamp(14, Timestamp.valueOf(feedback.getCriadoEm()));

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    feedback.setId(rs.getLong("id"));
                }

                return feedback;

            } catch (SQLException e) {
                throw new RuntimeException("Erro ao inserir feedback_ia: " + e.getMessage(), e);
            }
        });
    }

    private FeedbackIA insertWithConnection(Connection conn, FeedbackIA feedback) throws SQLException {
        String sql = "INSERT INTO feedback_ia (id_gravacao, id_empresa, pontos_fortes, pontos_fracos, sugestoes, " +
                "sentiment_score, probabilidade_fechamento, categoria_ambiental, " +
                "qualidade_atendimento, aderencia_script, gestao_objecoes, " +
                "objecoes_identificadas, momentos_chave, criado_em) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            LocalDateTime now = LocalDateTime.now();
            feedback.setCriadoEm(now);

            stmt.setLong(1, feedback.getIdGravacao());
            stmt.setLong(2, feedback.getIdEmpresa());

            stmt.setLong(2, feedback.getIdEmpresa());

            String[] pontosFortes = feedback.getPontosFortes().toArray(new String[0]);
            String[] pontosFracos = feedback.getPontosFracos().toArray(new String[0]);
            String[] sugestoes = feedback.getSugestoes().toArray(new String[0]);
            String[] objecoes = feedback.getObjecoesIdentificadas().toArray(new String[0]);
            String[] momentos = feedback.getMomentosChave().toArray(new String[0]);

            Array pontosFortesSql = conn.createArrayOf("TEXT", pontosFortes);
            Array pontosFracosSql = conn.createArrayOf("TEXT", pontosFracos);
            Array sugestoesSql = conn.createArrayOf("TEXT", sugestoes);
            Array objecoesSql = conn.createArrayOf("TEXT", objecoes);
            Array momentosSql = conn.createArrayOf("TEXT", momentos);

            stmt.setArray(3, pontosFortesSql);
            stmt.setArray(4, pontosFracosSql);
            stmt.setArray(5, sugestoesSql);
            stmt.setObject(6, feedback.getSentimentScore(), Types.INTEGER);
            stmt.setObject(7, feedback.getProbabilidadeFechamento(), Types.INTEGER);
            stmt.setString(8, feedback.getCategoriaAmbiental().name());
            stmt.setObject(9, feedback.getQualidadeAtendimento(), Types.INTEGER);
            stmt.setObject(10, feedback.getAderenciaScript(), Types.INTEGER);
            stmt.setObject(11, feedback.getGestaoObjecoes(), Types.INTEGER);
            stmt.setArray(12, objecoesSql);
            stmt.setArray(13, momentosSql);
            stmt.setTimestamp(14, Timestamp.valueOf(feedback.getCriadoEm()));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                feedback.setId(rs.getLong("id"));
            }

            return feedback;

        } catch (SQLException e) {
            throw new SQLException("Erro ao inserir feedback_ia: " + e.getMessage(), e);
        }
    }

    private FeedbackIA update(FeedbackIA feedback) {
        return TransactionManager.executeTransaction(conn -> {
            String sql = "UPDATE feedback_ia SET id_gravacao = ?, id_empresa = ?, pontos_fortes = ?, pontos_fracos = ?, " +
                    "sugestoes = ?, sentiment_score = ?, probabilidade_fechamento = ?, " +
                    "categoria_ambiental = ?, qualidade_atendimento = ?, aderencia_script = ?, " +
                    "gestao_objecoes = ?, objecoes_identificadas = ?, momentos_chave = ? " +
                    "WHERE id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setLong(1, feedback.getIdGravacao());
                stmt.setLong(2, feedback.getIdEmpresa());

                stmt.setLong(2, feedback.getIdEmpresa());

                String[] pontosFortes = feedback.getPontosFortes().toArray(new String[0]);
                String[] pontosFracos = feedback.getPontosFracos().toArray(new String[0]);
                String[] sugestoes = feedback.getSugestoes().toArray(new String[0]);
                String[] objecoes = feedback.getObjecoesIdentificadas().toArray(new String[0]);
                String[] momentos = feedback.getMomentosChave().toArray(new String[0]);

                Array pontosFortesSql = conn.createArrayOf("TEXT", pontosFortes);
                Array pontosFracosSql = conn.createArrayOf("TEXT", pontosFracos);
                Array sugestoesSql = conn.createArrayOf("TEXT", sugestoes);
                Array objecoesSql = conn.createArrayOf("TEXT", objecoes);
                Array momentosSql = conn.createArrayOf("TEXT", momentos);

                stmt.setArray(3, pontosFortesSql);
                stmt.setArray(4, pontosFracosSql);
                stmt.setArray(5, sugestoesSql);
                stmt.setObject(6, feedback.getSentimentScore(), Types.INTEGER);
                stmt.setObject(7, feedback.getProbabilidadeFechamento(), Types.INTEGER);
                stmt.setString(8, feedback.getCategoriaAmbiental().name());
                stmt.setObject(9, feedback.getQualidadeAtendimento(), Types.INTEGER);
                stmt.setObject(10, feedback.getAderenciaScript(), Types.INTEGER);
                stmt.setObject(11, feedback.getGestaoObjecoes(), Types.INTEGER);
                stmt.setArray(12, objecoesSql);
                stmt.setArray(13, momentosSql);
                stmt.setLong(14, feedback.getId());

                stmt.executeUpdate();
                return feedback;

            } catch (SQLException e) {
                throw new RuntimeException("Error updating feedback_ia: " + e.getMessage(), e);
            }
        });
    }

    private FeedbackIA updateWithConnection(Connection conn, FeedbackIA feedback) throws SQLException {
        String sql = "UPDATE feedback_ia SET id_gravacao = ?, id_empresa = ?, pontos_fortes = ?, pontos_fracos = ?, " +
                "sugestoes = ?, sentiment_score = ?, probabilidade_fechamento = ?, " +
                "categoria_ambiental = ?, qualidade_atendimento = ?, aderencia_script = ?, " +
                "gestao_objecoes = ?, objecoes_identificadas = ?, momentos_chave = ? " +
                "WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, feedback.getIdGravacao());
            stmt.setLong(2, feedback.getIdEmpresa());

            stmt.setLong(2, feedback.getIdEmpresa());

            String[] pontosFortes = feedback.getPontosFortes().toArray(new String[0]);
            String[] pontosFracos = feedback.getPontosFracos().toArray(new String[0]);
            String[] sugestoes = feedback.getSugestoes().toArray(new String[0]);
            String[] objecoes = feedback.getObjecoesIdentificadas().toArray(new String[0]);
            String[] momentos = feedback.getMomentosChave().toArray(new String[0]);

            Array pontosFortesSql = conn.createArrayOf("TEXT", pontosFortes);
            Array pontosFracosSql = conn.createArrayOf("TEXT", pontosFracos);
            Array sugestoesSql = conn.createArrayOf("TEXT", sugestoes);
            Array objecoesSql = conn.createArrayOf("TEXT", objecoes);
            Array momentosSql = conn.createArrayOf("TEXT", momentos);

            stmt.setArray(3, pontosFortesSql);
            stmt.setArray(4, pontosFracosSql);
            stmt.setArray(5, sugestoesSql);
            stmt.setObject(6, feedback.getSentimentScore(), Types.INTEGER);
            stmt.setObject(7, feedback.getProbabilidadeFechamento(), Types.INTEGER);
            stmt.setString(8, feedback.getCategoriaAmbiental().name());
            stmt.setObject(9, feedback.getQualidadeAtendimento(), Types.INTEGER);
            stmt.setObject(10, feedback.getAderenciaScript(), Types.INTEGER);
            stmt.setObject(11, feedback.getGestaoObjecoes(), Types.INTEGER);
            stmt.setArray(12, objecoesSql);
            stmt.setArray(13, momentosSql);
            stmt.setLong(14, feedback.getId());

            stmt.executeUpdate();
            return feedback;

        } catch (SQLException e) {
            throw new SQLException("Erro ao atualizar feedback_ia: " + e.getMessage(), e);
        }
    }

    public Optional<FeedbackIA> findById(Long id) {
        String sql = "SELECT * FROM feedback_ia WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToFeedbackIA(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar feedback por id: " + e.getMessage(), e);
        }
    }

    public Optional<FeedbackIA> findByGravacaoId(Long idGravacao) {
        String sql = "SELECT * FROM feedback_ia WHERE id_gravacao = ?";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, idGravacao);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToFeedbackIA(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar feedback por id_gravacao: " + e.getMessage(), e);
        }
    }

    public List<FeedbackIA> findAll() {
        String sql = "SELECT * FROM feedback_ia ORDER BY id";
        List<FeedbackIA> feedbacks = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                feedbacks.add(mapResultSetToFeedbackIA(rs));
            }
            return feedbacks;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar todos os feedbacks: " + e.getMessage(), e);
        }
    }

    public Optional<FeedbackIA> findByGravacao(Long idGravacao) {
        String sql = "SELECT * FROM feedback_ia WHERE id_gravacao = ?";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, idGravacao);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToFeedbackIA(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar feedback por gravação: " + e.getMessage(), e);
        }
    }

    public List<FeedbackIA> findByEmpresa(Long idEmpresa) {
        String sql = "SELECT * FROM feedback_ia WHERE id_empresa = ? ORDER BY criado_em DESC";
        List<FeedbackIA> feedbacks = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, idEmpresa);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                feedbacks.add(mapResultSetToFeedbackIA(rs));
            }
            return feedbacks;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar feedbacks por empresa: " + e.getMessage(), e);
        }
    }

    public List<FeedbackIA> findBySentimentScoreGreaterThan(Integer minScore) {
        String sql = "SELECT * FROM feedback_ia WHERE sentiment_score >= ? ORDER BY sentiment_score DESC";
        List<FeedbackIA> feedbacks = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, minScore);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                feedbacks.add(mapResultSetToFeedbackIA(rs));
            }
            return feedbacks;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar feedbacks por pontuação de sentimento: " + e.getMessage(), e);
        }
    }

    public List<FeedbackIA> findByProbabilidadeFechamentoGreaterThan(Integer minProbabilidade) {
        String sql = "SELECT * FROM feedback_ia WHERE probabilidade_fechamento >= ? " +
                "ORDER BY probabilidade_fechamento DESC";
        List<FeedbackIA> feedbacks = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, minProbabilidade);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                feedbacks.add(mapResultSetToFeedbackIA(rs));
            }
            return feedbacks;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar feedbacks por probabilidade de fechamento: " + e.getMessage(), e);
        }
    }

    public void deleteById(Long id) {
        TransactionManager.executeTransactionVoid(conn -> {
            String sql = "DELETE FROM feedback_ia WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, id);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao excluir feedback: " + e.getMessage(), e);
            }
        });
    }

    public boolean existsById(Long id) {
        String sql = "SELECT EXISTS(SELECT 1 FROM feedback_ia WHERE id = ?)";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBoolean(1);
            }
            return false;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao verificar se feedback existe: " + e.getMessage(), e);
        }
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM feedback_ia";

        try (Connection conn = DatabaseConfig.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar feedbacks: " + e.getMessage(), e);
        }
    }

    private FeedbackIA mapResultSetToFeedbackIA(ResultSet rs) throws SQLException {
        FeedbackIA feedback = new FeedbackIA();
        feedback.setId(rs.getLong("id"));
        feedback.setIdGravacao(rs.getLong("id_gravacao"));
        feedback.setIdEmpresa(rs.getLong("id_empresa"));

        Array pontosFortesSql = rs.getArray("pontos_fortes");
        Array pontosFracosSql = rs.getArray("pontos_fracos");
        Array sugestoesSql = rs.getArray("sugestoes");

        feedback.setPontosFortes(
                pontosFortesSql != null ? Arrays.asList((String[]) pontosFortesSql.getArray()) : new ArrayList<>());
        feedback.setPontosFracos(
                pontosFracosSql != null ? Arrays.asList((String[]) pontosFracosSql.getArray()) : new ArrayList<>());
        feedback.setSugestoes(
                sugestoesSql != null ? Arrays.asList((String[]) sugestoesSql.getArray()) : new ArrayList<>());

        Integer sentiment = (Integer) rs.getObject("sentiment_score");
        Integer probabilidade = (Integer) rs.getObject("probabilidade_fechamento");
        Integer qualidade = (Integer) rs.getObject("qualidade_atendimento");
        Integer aderencia = (Integer) rs.getObject("aderencia_script");
        Integer gestao = (Integer) rs.getObject("gestao_objecoes");

        Array objecoesIdentificadasSql = rs.getArray("objecoes_identificadas");
        Array momentosChaveSql = rs.getArray("momentos_chave");

        feedback.setSentimentScore(sentiment);
        feedback.setProbabilidadeFechamento(probabilidade);
        feedback.setQualidadeAtendimento(qualidade);
        feedback.setAderenciaScript(aderencia);
        feedback.setGestaoObjecoes(gestao);
        feedback.setObjecoesIdentificadas(
                objecoesIdentificadasSql != null ? Arrays.asList((String[]) objecoesIdentificadasSql.getArray())
                        : new ArrayList<>());
        feedback.setMomentosChave(
                momentosChaveSql != null ? Arrays.asList((String[]) momentosChaveSql.getArray()) : new ArrayList<>());

        String categoriaStr = rs.getString("categoria_ambiental");
        if (categoriaStr != null) {
            try {
                feedback.setCategoriaAmbiental(CategoriaAmbiental.valueOf(categoriaStr));
            } catch (IllegalArgumentException e) {
                feedback.setCategoriaAmbiental(CategoriaAmbiental.NEUTRO);
            }
        }

        feedback.setCriadoEm(rs.getTimestamp("criado_em").toLocalDateTime());

        return feedback;
    }
}
