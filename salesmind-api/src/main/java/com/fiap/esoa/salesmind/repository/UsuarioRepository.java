package com.fiap.esoa.salesmind.repository;

import com.fiap.esoa.salesmind.config.DatabaseConfig;
import com.fiap.esoa.salesmind.enums.Funcao;
import com.fiap.esoa.salesmind.model.Usuario;
import com.fiap.esoa.salesmind.util.TransactionManager;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class UsuarioRepository {

    public Usuario save(Usuario usuario) {
        if (usuario.getId() == null) {
            return insert(usuario);
        } else {
            return update(usuario);
        }
    }

    private Usuario insert(Usuario usuario) {
        return TransactionManager.executeTransaction(conn -> {
            String sql = "INSERT INTO usuario (id_empresa, nome, email, funcao, senha, criado_em, atualizado_em) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                LocalDateTime now = LocalDateTime.now();
                usuario.setCriadoEm(now);
                usuario.setAtualizadoEm(now);

                stmt.setLong(1, usuario.getIdEmpresa());
                stmt.setString(2, usuario.getNome());
                stmt.setString(3, usuario.getEmail());
                stmt.setString(4, usuario.getFuncao().name());
                stmt.setString(5, usuario.getSenha());
                stmt.setTimestamp(6, Timestamp.valueOf(usuario.getCriadoEm()));
                stmt.setTimestamp(7, Timestamp.valueOf(usuario.getAtualizadoEm()));

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    usuario.setId(rs.getLong("id"));
                }

                return usuario;

            } catch (SQLException e) {
                throw new RuntimeException("Erro ao inserir usuário: " + e.getMessage(), e);
            }
        });
    }

    private Usuario update(Usuario usuario) {
        return TransactionManager.executeTransaction(conn -> {
            String sql = "UPDATE usuario SET id_empresa = ?, nome = ?, email = ?, funcao = ?, " +
                    "senha = ?, atualizado_em = ? WHERE id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                usuario.setAtualizadoEm(LocalDateTime.now());

                stmt.setLong(1, usuario.getIdEmpresa());
                stmt.setString(2, usuario.getNome());
                stmt.setString(3, usuario.getEmail());
                stmt.setString(4, usuario.getFuncao().name());
                stmt.setString(5, usuario.getSenha());
                stmt.setTimestamp(6, Timestamp.valueOf(usuario.getAtualizadoEm()));
                stmt.setLong(7, usuario.getId());

                stmt.executeUpdate();
                return usuario;

            } catch (SQLException e) {
                throw new RuntimeException("Erro ao atualizar usuário: " + e.getMessage(), e);
            }
        });
    }

    public Optional<Usuario> findById(Long id) {
        String sql = "SELECT * FROM usuario WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToUsuario(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar usuário por id: " + e.getMessage(), e);
        }
    }

    public List<Usuario> findAll() {
        String sql = "SELECT * FROM usuario ORDER BY id";
        List<Usuario> usuarios = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                usuarios.add(mapResultSetToUsuario(rs));
            }
            return usuarios;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar todos os usuários: " + e.getMessage(), e);
        }
    }

    public List<Usuario> findByEmpresa(Long idEmpresa) {
        String sql = "SELECT * FROM usuario WHERE id_empresa = ? ORDER BY id";
        List<Usuario> usuarios = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, idEmpresa);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                usuarios.add(mapResultSetToUsuario(rs));
            }
            return usuarios;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar usuários por empresa: " + e.getMessage(), e);
        }
    }

    public Optional<Usuario> findByEmail(String email) {
        String sql = "SELECT * FROM usuario WHERE email = ?";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToUsuario(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar usuário por email: " + e.getMessage(), e);
        }
    }

    public List<Usuario> findByFuncao(Funcao funcao) {
        String sql = "SELECT * FROM usuario WHERE funcao = ? ORDER BY id";
        List<Usuario> usuarios = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, funcao.name());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                usuarios.add(mapResultSetToUsuario(rs));
            }
            return usuarios;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar usuários por função: " + e.getMessage(), e);
        }
    }

    public void deleteById(Long id) {
        TransactionManager.executeTransactionVoid(conn -> {
            String sql = "DELETE FROM usuario WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, id);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao excluir usuário: " + e.getMessage(), e);
            }
        });
    }

    public boolean existsById(Long id) {
        String sql = "SELECT EXISTS(SELECT 1 FROM usuario WHERE id = ?)";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBoolean(1);
            }
            return false;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao verificar se usuário existe: " + e.getMessage(), e);
        }
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM usuario";

        try (Connection conn = DatabaseConfig.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar usuários: " + e.getMessage(), e);
        }
    }

    public long countByEmpresa(Long idEmpresa) {
        String sql = "SELECT COUNT(*) FROM usuario WHERE id_empresa = ?";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, idEmpresa);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar usuários por empresa: " + e.getMessage(), e);
        }
    }

    private Usuario mapResultSetToUsuario(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setId(rs.getLong("id"));
        usuario.setIdEmpresa(rs.getLong("id_empresa"));
        usuario.setNome(rs.getString("nome"));
        usuario.setEmail(rs.getString("email"));
        usuario.setFuncao(Funcao.valueOf(rs.getString("funcao")));
        usuario.setSenha(rs.getString("senha"));
        usuario.setCriadoEm(rs.getTimestamp("criado_em").toLocalDateTime());
        usuario.setAtualizadoEm(rs.getTimestamp("atualizado_em").toLocalDateTime());
        return usuario;
    }
}
