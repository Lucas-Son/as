package com.fiap.esoa.salesmind.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConfig {

    private static HikariDataSource dataSource;

    static {
        initializeDataSource();
    }

    private static void initializeDataSource() {
        HikariConfig config = new HikariConfig();

        String dbUrl = System.getProperty("DB_URL", System.getenv("DB_URL"));
        String dbUser = System.getProperty("DB_USER", System.getenv("DB_USER"));
        String dbPassword = System.getProperty("DB_PASSWORD", System.getenv("DB_PASSWORD"));

        if (dbUrl == null || dbUrl.isEmpty()) {
            dbUrl = "jdbc:postgresql://localhost:5432/salesmind";
            System.out.println("AVISO: DB_URL não configurada, usando padrão: " + dbUrl);
        }
        if (dbUser == null || dbUser.isEmpty()) {
            dbUser = "postgres";
            System.out.println("AVISO: DB_USER não configurado, usando padrão: " + dbUser);
        }
        if (dbPassword == null || dbPassword.isEmpty()) {
            dbPassword = "postgres";
            System.out.println("AVISO: DB_PASSWORD não configurada, usando padrão");
        }

        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        dataSource = new HikariDataSource(config);

        System.out.println("Pool de conexões do banco de dados inicializado");
        initializeSchema();
    }

    public static DataSource getDataSource() {
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private static void initializeSchema() {
        String[] createTableStatements = {
                """
                        CREATE TABLE IF NOT EXISTS empresa (
                            id SERIAL PRIMARY KEY,
                            nome_empresa VARCHAR(255) NOT NULL,
                            cnpj VARCHAR(18) UNIQUE NOT NULL,
                            tipo_conta VARCHAR(50) NOT NULL,
                            criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            atualizado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                        """,
                """
                        CREATE TABLE IF NOT EXISTS usuario (
                            id SERIAL PRIMARY KEY,
                            id_empresa INTEGER NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
                            nome VARCHAR(255) NOT NULL,
                            email VARCHAR(255) UNIQUE NOT NULL,
                            senha VARCHAR(255),
                            funcao VARCHAR(50) NOT NULL,
                            criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            atualizado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                        """,
                """
                        CREATE TABLE IF NOT EXISTS cliente (
                            id SERIAL PRIMARY KEY,
                            id_empresa INTEGER NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
                            nome VARCHAR(255) NOT NULL,
                            cpf_cnpj VARCHAR(18),
                            telefone VARCHAR(20),
                            email VARCHAR(255),
                            segmento VARCHAR(100),
                            criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            atualizado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                        """,
                """
                        CREATE TABLE IF NOT EXISTS gravacao_call (
                            id SERIAL PRIMARY KEY,
                            id_usuario INTEGER NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                            id_cliente INTEGER NOT NULL REFERENCES cliente(id) ON DELETE CASCADE,
                            audio_url VARCHAR(500),
                            audio_filename VARCHAR(255),
                            transcricao TEXT,
                            resumo_ia TEXT,
                            status_venda VARCHAR(50) DEFAULT 'PENDENTE',
                            status_processamento VARCHAR(50) DEFAULT 'UPLOADING',
                            duracao_segundos INTEGER,
                            erro_processamento TEXT,
                            data_gravacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            atualizado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                        """,
                """
                        CREATE TABLE IF NOT EXISTS feedback_ia (
                            id SERIAL PRIMARY KEY,
                            id_gravacao INTEGER NOT NULL REFERENCES gravacao_call(id) ON DELETE CASCADE,
                            id_empresa INTEGER NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
                            pontos_fortes TEXT[],
                            pontos_fracos TEXT[],
                            sugestoes TEXT[],
                            sentiment_score INTEGER CHECK (sentiment_score >= 0 AND sentiment_score <= 100),
                            probabilidade_fechamento INTEGER CHECK (probabilidade_fechamento >= 0 AND probabilidade_fechamento <= 100),
                            categoria_ambiental VARCHAR(50),
                            qualidade_atendimento INTEGER CHECK (qualidade_atendimento >= 0 AND qualidade_atendimento <= 100),
                            aderencia_script INTEGER CHECK (aderencia_script >= 0 AND aderencia_script <= 100),
                            gestao_objecoes INTEGER CHECK (gestao_objecoes >= 0 AND gestao_objecoes <= 100),
                            objecoes_identificadas TEXT[],
                            momentos_chave TEXT[],
                            tipo_feedback VARCHAR(50) DEFAULT 'AUTOMATICO',
                            criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            UNIQUE(id_gravacao)
                        )
                        """,
                """
                        -- View: Estatísticas agregadas por empresa
                        CREATE OR REPLACE VIEW v_estatisticas_empresa AS
                        SELECT 
                            e.id AS id_empresa,
                            COUNT(DISTINCT u.id) AS total_usuarios,
                            COUNT(DISTINCT c.id) AS total_clientes,
                            COUNT(g.id) AS total_gravacoes,
                            COUNT(CASE WHEN g.status_venda = 'FECHADO' THEN 1 END) AS vendas_fechadas,
                            CASE 
                                WHEN COUNT(g.id) > 0 
                                THEN ROUND((COUNT(CASE WHEN g.status_venda = 'FECHADO' THEN 1 END)::NUMERIC / COUNT(g.id)::NUMERIC) * 100, 2)
                                ELSE 0
                            END AS taxa_conversao
                        FROM empresa e
                        LEFT JOIN usuario u ON u.id_empresa = e.id
                        LEFT JOIN cliente c ON c.id_empresa = e.id
                        LEFT JOIN gravacao_call g ON g.id_usuario = u.id
                        GROUP BY e.id
                        """,
                """
                        -- View: Performance individual de vendedores
                        CREATE OR REPLACE VIEW v_performance_vendedores AS
                        SELECT 
                            u.id AS id_usuario,
                            u.nome AS nome_usuario,
                            u.email,
                            e.id AS id_empresa,
                            e.nome_empresa,
                            COUNT(g.id) AS total_gravacoes,
                            COUNT(CASE WHEN g.status_venda = 'FECHADO' THEN 1 END) AS vendas_fechadas,
                            CASE 
                                WHEN COUNT(g.id) > 0 
                                THEN ROUND((COUNT(CASE WHEN g.status_venda = 'FECHADO' THEN 1 END)::NUMERIC / COUNT(g.id)::NUMERIC) * 100, 2)
                                ELSE 0
                            END AS taxa_conversao,
                            ROUND(AVG(f.sentiment_score), 2) AS media_sentiment,
                            ROUND(AVG(f.qualidade_atendimento), 2) AS media_qualidade
                        FROM usuario u
                        INNER JOIN empresa e ON e.id = u.id_empresa
                        LEFT JOIN gravacao_call g ON g.id_usuario = u.id
                        LEFT JOIN feedback_ia f ON f.id_gravacao = g.id
                        GROUP BY u.id, u.nome, u.email, e.id, e.nome_empresa
                        """,
                """
                        -- View: Gravações completas com dados relacionados
                        CREATE OR REPLACE VIEW v_gravacoes_completas AS
                        SELECT 
                            g.id AS id_gravacao,
                            g.data_gravacao,
                            g.duracao_segundos,
                            g.status_venda,
                            g.status_processamento,
                            u.id AS id_usuario,
                            u.nome AS nome_usuario,
                            u.email AS email_usuario,
                            c.id AS id_cliente,
                            c.nome AS nome_cliente,
                            c.telefone AS telefone_cliente,
                            e.id AS id_empresa,
                            e.nome_empresa,
                            f.id AS id_feedback,
                            f.sentiment_score,
                            f.probabilidade_fechamento,
                            f.qualidade_atendimento,
                            f.categoria_ambiental
                        FROM gravacao_call g
                        INNER JOIN usuario u ON u.id = g.id_usuario
                        INNER JOIN cliente c ON c.id = g.id_cliente
                        INNER JOIN empresa e ON e.id = u.id_empresa
                        LEFT JOIN feedback_ia f ON f.id_gravacao = g.id
                        """
        };

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            for (String sql : createTableStatements) {
                stmt.execute(sql);
            }
            
            System.out.println("Schema do banco de dados inicializado com sucesso");
        } catch (SQLException e) {
            System.err.println("Falha ao inicializar schema do banco de dados: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("Pool de conexões do banco de dados fechado");
        }
    }
}
