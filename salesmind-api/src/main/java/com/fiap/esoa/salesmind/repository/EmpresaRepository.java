package com.fiap.esoa.salesmind.repository;

import com.fiap.esoa.salesmind.config.DatabaseConfig;
import com.fiap.esoa.salesmind.dto.EstatisticasEmpresaDTO;
import com.fiap.esoa.salesmind.model.Empresa;
import com.fiap.esoa.salesmind.util.TransactionManager;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class EmpresaRepository {

    public Empresa save(Empresa empresa) {
        if (empresa.getId() == null) {
            return insert(empresa);
        } else {
            return update(empresa);
        }
    }

    private Empresa insert(Empresa empresa) {
        return TransactionManager.executeTransaction(conn -> {
            String sql = "INSERT INTO empresa (nome_empresa, cnpj, criado_em, atualizado_em) " +
                    "VALUES (?, ?, ?, ?) RETURNING id";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                LocalDateTime now = LocalDateTime.now();
                empresa.setCriadoEm(now);
                empresa.setAtualizadoEm(now);

                stmt.setString(1, empresa.getNomeEmpresa());
                stmt.setString(2, empresa.getCnpj());
                stmt.setTimestamp(3, Timestamp.valueOf(empresa.getCriadoEm()));
                stmt.setTimestamp(4, Timestamp.valueOf(empresa.getAtualizadoEm()));

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    empresa.setId(rs.getLong("id"));
                }

                return empresa;

            } catch (SQLException e) {
                throw new RuntimeException("Erro ao inserir empresa: " + e.getMessage(), e);
            }
        });
    }

    private Empresa update(Empresa empresa) {
        return TransactionManager.executeTransaction(conn -> {
            String sql = "UPDATE empresa SET nome_empresa = ?, cnpj = ?, atualizado_em = ? " +
                    "WHERE id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                empresa.setAtualizadoEm(LocalDateTime.now());

                stmt.setString(1, empresa.getNomeEmpresa());
                stmt.setString(2, empresa.getCnpj());
                stmt.setTimestamp(3, Timestamp.valueOf(empresa.getAtualizadoEm()));
                stmt.setLong(4, empresa.getId());

                stmt.executeUpdate();
                return empresa;

            } catch (SQLException e) {
                throw new RuntimeException("Erro ao atualizar empresa: " + e.getMessage(), e);
            }
        });
    }

    public Optional<Empresa> findById(Long id) {
        String sql = "SELECT * FROM empresa WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToEmpresa(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar empresa por id: " + e.getMessage(), e);
        }
    }

    public List<Empresa> findAll() {
        String sql = "SELECT * FROM empresa ORDER BY id";
        List<Empresa> empresas = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                empresas.add(mapResultSetToEmpresa(rs));
            }
            return empresas;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar todas as empresas: " + e.getMessage(), e);
        }
    }

    public Optional<Empresa> findByCnpj(String cnpj) {
        String sql = "SELECT * FROM empresa WHERE cnpj = ?";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cnpj);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToEmpresa(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar empresa por cnpj: " + e.getMessage(), e);
        }
    }

    public void deleteById(Long id) {
        TransactionManager.executeTransactionVoid(conn -> {
            String sql = "DELETE FROM empresa WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, id);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao excluir empresa: " + e.getMessage(), e);
            }
        });
    }

    public boolean existsById(Long id) {
        String sql = "SELECT EXISTS(SELECT 1 FROM empresa WHERE id = ?)";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBoolean(1);
            }
            return false;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao verificar se empresa existe: " + e.getMessage(), e);
        }
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM empresa";

        try (Connection conn = DatabaseConfig.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar empresas: " + e.getMessage(), e);
        }
    }

    public EstatisticasEmpresaDTO getEstatisticas(Long idEmpresa) {
        String sql = "SELECT id_empresa, total_usuarios, total_clientes, total_gravacoes, " +
                     "vendas_fechadas, taxa_conversao " +
                     "FROM v_estatisticas_empresa WHERE id_empresa = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, idEmpresa);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new EstatisticasEmpresaDTO(
                            rs.getLong("id_empresa"),
                            rs.getLong("total_usuarios"),
                            rs.getLong("total_clientes"),
                            rs.getLong("total_gravacoes"),
                            rs.getLong("vendas_fechadas"),
                            rs.getDouble("taxa_conversao")
                    );
                }
                return null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar estatísticas da empresa: " + e.getMessage(), e);
        }
    }

    private Empresa mapResultSetToEmpresa(ResultSet rs) throws SQLException {
        Empresa empresa = new Empresa();
        empresa.setId(rs.getLong("id"));
        empresa.setNomeEmpresa(rs.getString("nome_empresa"));
        empresa.setCnpj(rs.getString("cnpj"));
        empresa.setCriadoEm(rs.getTimestamp("criado_em").toLocalDateTime());
        empresa.setAtualizadoEm(rs.getTimestamp("atualizado_em").toLocalDateTime());
        return empresa;
    }
}
