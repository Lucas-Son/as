package com.fiap.esoa.salesmind.repository;

import com.fiap.esoa.salesmind.config.DatabaseConfig;
import com.fiap.esoa.salesmind.model.Cliente;
import com.fiap.esoa.salesmind.util.TransactionManager;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class ClienteRepository {

    public Cliente save(Cliente cliente) {
        if (cliente.getId() == null) {
            return insert(cliente);
        } else {
            return update(cliente);
        }
    }

    private Cliente insert(Cliente cliente) {
        return TransactionManager.executeTransaction(conn -> {
            String sql = "INSERT INTO cliente (id_empresa, nome, cpf_cnpj, telefone, email, segmento, " +
                    "criado_em, atualizado_em) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                LocalDateTime now = LocalDateTime.now();
                cliente.setCriadoEm(now);
                cliente.setAtualizadoEm(now);

                stmt.setLong(1, cliente.getIdEmpresa());
                stmt.setString(2, cliente.getNome());
                stmt.setString(3, cliente.getCpfCnpj());
                stmt.setString(4, cliente.getTelefone());
                stmt.setString(5, cliente.getEmail());
                stmt.setString(6, cliente.getSegmento());
                stmt.setTimestamp(7, Timestamp.valueOf(cliente.getCriadoEm()));
                stmt.setTimestamp(8, Timestamp.valueOf(cliente.getAtualizadoEm()));

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    cliente.setId(rs.getLong("id"));
                }

                return cliente;

            } catch (SQLException e) {
                throw new RuntimeException("Erro ao inserir cliente: " + e.getMessage(), e);
            }
        });
    }

    private Cliente update(Cliente cliente) {
        return TransactionManager.executeTransaction(conn -> {
            String sql = "UPDATE cliente SET id_empresa = ?, nome = ?, cpf_cnpj = ?, telefone = ?, " +
                    "email = ?, segmento = ?, atualizado_em = ? WHERE id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                cliente.setAtualizadoEm(LocalDateTime.now());

                stmt.setLong(1, cliente.getIdEmpresa());
                stmt.setString(2, cliente.getNome());
                stmt.setString(3, cliente.getCpfCnpj());
                stmt.setString(4, cliente.getTelefone());
                stmt.setString(5, cliente.getEmail());
                stmt.setString(6, cliente.getSegmento());
                stmt.setTimestamp(7, Timestamp.valueOf(cliente.getAtualizadoEm()));
                stmt.setLong(8, cliente.getId());

                stmt.executeUpdate();
                return cliente;

            } catch (SQLException e) {
                throw new RuntimeException("Erro ao atualizar cliente: " + e.getMessage(), e);
            }
        });
    }

    public Optional<Cliente> findById(Long id) {
        String sql = "SELECT * FROM cliente WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToCliente(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar cliente por id: " + e.getMessage(), e);
        }
    }

    public List<Cliente> findAll() {
        String sql = "SELECT * FROM cliente ORDER BY id";
        List<Cliente> clientes = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                clientes.add(mapResultSetToCliente(rs));
            }
            return clientes;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar todos os clientes: " + e.getMessage(), e);
        }
    }

    public List<Cliente> findByEmpresa(Long idEmpresa) {
        String sql = "SELECT * FROM cliente WHERE id_empresa = ? ORDER BY id";
        List<Cliente> clientes = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, idEmpresa);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                clientes.add(mapResultSetToCliente(rs));
            }
            return clientes;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar clientes por empresa: " + e.getMessage(), e);
        }
    }

    public Optional<Cliente> findByCpfCnpj(String cpfCnpj) {
        String sql = "SELECT * FROM cliente WHERE cpf_cnpj = ?";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cpfCnpj);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToCliente(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar cliente por cpf_cnpj: " + e.getMessage(), e);
        }
    }

    public List<Cliente> findBySegmento(String segmento) {
        String sql = "SELECT * FROM cliente WHERE segmento = ? ORDER BY id";
        List<Cliente> clientes = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, segmento);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                clientes.add(mapResultSetToCliente(rs));
            }
            return clientes;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar clientes por segmento: " + e.getMessage(), e);
        }
    }

    public Optional<Cliente> findByEmail(String email) {
        String sql = "SELECT * FROM cliente WHERE email = ?";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToCliente(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar cliente por email: " + e.getMessage(), e);
        }
    }

    public Optional<Cliente> findByTelefone(String telefone) {
        String sql = "SELECT * FROM cliente WHERE telefone = ?";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, telefone);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToCliente(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar cliente por telefone: " + e.getMessage(), e);
        }
    }

    public void deleteById(Long id) {
        TransactionManager.executeTransactionVoid(conn -> {
            String sql = "DELETE FROM cliente WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, id);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao excluir cliente: " + e.getMessage(), e);
            }
        });
    }

    public boolean existsById(Long id) {
        String sql = "SELECT EXISTS(SELECT 1 FROM cliente WHERE id = ?)";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBoolean(1);
            }
            return false;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao verificar se cliente existe: " + e.getMessage(), e);
        }
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM cliente";

        try (Connection conn = DatabaseConfig.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar clientes: " + e.getMessage(), e);
        }
    }

    public long countByEmpresa(Long idEmpresa) {
        String sql = "SELECT COUNT(*) FROM cliente WHERE id_empresa = ?";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, idEmpresa);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar clientes por empresa: " + e.getMessage(), e);
        }
    }

    private Cliente mapResultSetToCliente(ResultSet rs) throws SQLException {
        Cliente cliente = new Cliente();
        cliente.setId(rs.getLong("id"));
        cliente.setIdEmpresa(rs.getLong("id_empresa"));
        cliente.setNome(rs.getString("nome"));
        cliente.setCpfCnpj(rs.getString("cpf_cnpj"));
        cliente.setTelefone(rs.getString("telefone"));
        cliente.setEmail(rs.getString("email"));
        cliente.setSegmento(rs.getString("segmento"));
        cliente.setCriadoEm(rs.getTimestamp("criado_em").toLocalDateTime());
        cliente.setAtualizadoEm(rs.getTimestamp("atualizado_em").toLocalDateTime());
        return cliente;
    }
}
