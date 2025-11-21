-- ============================================
-- SalesMind Database Schema
-- ============================================

-- Limpar schema existente (CUIDADO: Apaga todos os dados!)
-- Descomente apenas se quiser recriar o banco do zero
-- DROP TABLE IF EXISTS feedback_ia CASCADE;
-- DROP TABLE IF EXISTS gravacao_call CASCADE;
-- DROP TABLE IF EXISTS cliente CASCADE;
-- DROP TABLE IF EXISTS usuario CASCADE;
-- DROP TABLE IF EXISTS empresa CASCADE;

-- ============================================
-- TABELA: EMPRESA
-- Armazena informações das empresas clientes
-- ============================================
CREATE TABLE IF NOT EXISTS empresa (
    id SERIAL PRIMARY KEY,
    nome_empresa VARCHAR(255) NOT NULL,
    cnpj VARCHAR(18) UNIQUE NOT NULL,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para empresa
CREATE INDEX IF NOT EXISTS idx_empresa_cnpj ON empresa(cnpj);

-- Comentários
COMMENT ON TABLE empresa IS 'Empresas clientes do sistema';
COMMENT ON COLUMN empresa.cnpj IS 'CNPJ único da empresa';

-- ============================================
-- TABELA: USUARIO
-- Armazena usuários (vendedores, gerentes, admins)
-- ============================================
CREATE TABLE IF NOT EXISTS usuario (
    id SERIAL PRIMARY KEY,
    id_empresa INTEGER NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    senha VARCHAR(255),
    funcao VARCHAR(50) NOT NULL CHECK (funcao IN ('ADMIN', 'GERENTE', 'VENDEDOR')),
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para usuario
CREATE INDEX IF NOT EXISTS idx_usuario_id_empresa ON usuario(id_empresa);
CREATE INDEX IF NOT EXISTS idx_usuario_email ON usuario(email);
CREATE INDEX IF NOT EXISTS idx_usuario_funcao ON usuario(funcao);

-- Comentários
COMMENT ON TABLE usuario IS 'Usuários do sistema (vendedores, gerentes, administradores)';
COMMENT ON COLUMN usuario.funcao IS 'Função: ADMIN, GERENTE ou VENDEDOR';
COMMENT ON COLUMN usuario.senha IS 'Senha hasheada com BCrypt';
COMMENT ON COLUMN usuario.id_empresa IS 'Empresa à qual o usuário pertence (multi-tenancy)';

-- ============================================
-- TABELA: CLIENTE
-- Armazena clientes dos vendedores
-- ============================================
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
);

-- Índices para cliente
CREATE INDEX IF NOT EXISTS idx_cliente_id_empresa ON cliente(id_empresa);
CREATE INDEX IF NOT EXISTS idx_cliente_cpf_cnpj ON cliente(cpf_cnpj);
CREATE INDEX IF NOT EXISTS idx_cliente_email ON cliente(email);
CREATE INDEX IF NOT EXISTS idx_cliente_segmento ON cliente(segmento);
CREATE INDEX IF NOT EXISTS idx_cliente_telefone ON cliente(telefone);

-- Comentários
COMMENT ON TABLE cliente IS 'Clientes das empresas (prospects ou clientes ativos)';
COMMENT ON COLUMN cliente.cpf_cnpj IS 'CPF ou CNPJ do cliente (opcional)';
COMMENT ON COLUMN cliente.segmento IS 'Segmento de mercado do cliente';
COMMENT ON COLUMN cliente.id_empresa IS 'Empresa à qual o cliente pertence (multi-tenancy)';

-- ============================================
-- TABELA: GRAVACAO_CALL
-- Armazena gravações de ligações de vendas
-- ============================================
CREATE TABLE IF NOT EXISTS gravacao_call (
    id SERIAL PRIMARY KEY,
    id_usuario INTEGER NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    id_cliente INTEGER NOT NULL REFERENCES cliente(id) ON DELETE CASCADE,
    audio_url VARCHAR(500),
    audio_filename VARCHAR(255),
    transcricao TEXT,
    resumo_ia TEXT,
    status_venda VARCHAR(50) DEFAULT 'PENDENTE' CHECK (status_venda IN ('PENDENTE', 'QUALIFICADO', 'PROPOSTA_ENVIADA', 'NEGOCIACAO', 'FECHADO', 'PERDIDO')),
    status_processamento VARCHAR(50) DEFAULT 'UPLOADING' CHECK (status_processamento IN ('UPLOADING', 'PENDENTE', 'PROCESSANDO', 'CONCLUIDO', 'ERRO')),
    duracao_segundos INTEGER,
    erro_processamento TEXT,
    data_gravacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para gravacao_call
CREATE INDEX IF NOT EXISTS idx_gravacao_id_usuario ON gravacao_call(id_usuario);
CREATE INDEX IF NOT EXISTS idx_gravacao_id_cliente ON gravacao_call(id_cliente);
CREATE INDEX IF NOT EXISTS idx_gravacao_status_venda ON gravacao_call(status_venda);
CREATE INDEX IF NOT EXISTS idx_gravacao_status_processamento ON gravacao_call(status_processamento);
CREATE INDEX IF NOT EXISTS idx_gravacao_data_gravacao ON gravacao_call(data_gravacao DESC);
CREATE INDEX IF NOT EXISTS idx_gravacao_composite_status ON gravacao_call(status_venda, status_processamento);

-- Comentários
COMMENT ON TABLE gravacao_call IS 'Gravações de ligações de vendas';
COMMENT ON COLUMN gravacao_call.status_venda IS 'PENDENTE: aguardando resultado, QUALIFICADO: lead qualificado, PROPOSTA_ENVIADA: proposta enviada, NEGOCIACAO: em negociação, FECHADO: venda realizada, PERDIDO: venda perdida';
COMMENT ON COLUMN gravacao_call.status_processamento IS 'UPLOADING: enviando arquivo, PENDENTE: aguardando processamento, PROCESSANDO: em análise, CONCLUIDO: processado, ERRO: falha';
COMMENT ON COLUMN gravacao_call.transcricao IS 'Transcrição gerada pela IA (Google Gemini)';
COMMENT ON COLUMN gravacao_call.resumo_ia IS 'Resumo da conversa gerado pela IA';
COMMENT ON COLUMN gravacao_call.duracao_segundos IS 'Duração da gravação em segundos';

-- ============================================
-- TABELA: FEEDBACK_IA
-- Armazena análises e feedbacks gerados pela IA
-- ============================================
CREATE TABLE IF NOT EXISTS feedback_ia (
    id SERIAL PRIMARY KEY,
    id_gravacao INTEGER NOT NULL REFERENCES gravacao_call(id) ON DELETE CASCADE,
    id_empresa INTEGER NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    pontos_fortes TEXT[],
    pontos_fracos TEXT[],
    sugestoes TEXT[],
    sentiment_score INTEGER CHECK (sentiment_score >= 0 AND sentiment_score <= 100),
    probabilidade_fechamento INTEGER CHECK (probabilidade_fechamento >= 0 AND probabilidade_fechamento <= 100),
    categoria_ambiental VARCHAR(50) CHECK (categoria_ambiental IN ('POSITIVO', 'NEUTRO', 'NEGATIVO')),
    qualidade_atendimento INTEGER CHECK (qualidade_atendimento >= 0 AND qualidade_atendimento <= 100),
    aderencia_script INTEGER CHECK (aderencia_script >= 0 AND aderencia_script <= 100),
    gestao_objecoes INTEGER CHECK (gestao_objecoes >= 0 AND gestao_objecoes <= 100),
    objecoes_identificadas TEXT[],
    momentos_chave TEXT[],
    tipo_feedback VARCHAR(50) DEFAULT 'AUTOMATICO',
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(id_gravacao)
);

-- Índices para feedback_ia
CREATE INDEX IF NOT EXISTS idx_feedback_id_gravacao ON feedback_ia(id_gravacao);
CREATE INDEX IF NOT EXISTS idx_feedback_id_empresa ON feedback_ia(id_empresa);
CREATE INDEX IF NOT EXISTS idx_feedback_categoria ON feedback_ia(categoria_ambiental);
CREATE INDEX IF NOT EXISTS idx_feedback_sentiment ON feedback_ia(sentiment_score DESC);
CREATE INDEX IF NOT EXISTS idx_feedback_probabilidade ON feedback_ia(probabilidade_fechamento DESC);

-- Comentários
COMMENT ON TABLE feedback_ia IS 'Análises e feedbacks gerados pela IA para cada gravação';
COMMENT ON COLUMN feedback_ia.pontos_fortes IS 'Array de pontos fortes identificados pela IA';
COMMENT ON COLUMN feedback_ia.pontos_fracos IS 'Array de pontos fracos identificados pela IA';
COMMENT ON COLUMN feedback_ia.sugestoes IS 'Array de sugestões de melhoria';
COMMENT ON COLUMN feedback_ia.sentiment_score IS 'Score de sentimento da conversa (0-100)';
COMMENT ON COLUMN feedback_ia.probabilidade_fechamento IS 'Probabilidade de fechamento da venda (0-100)';
COMMENT ON COLUMN feedback_ia.categoria_ambiental IS 'POSITIVO: clima favorável, NEUTRO: clima neutro, NEGATIVO: clima desfavorável';
COMMENT ON COLUMN feedback_ia.qualidade_atendimento IS 'Score de qualidade do atendimento (0-100)';
COMMENT ON COLUMN feedback_ia.aderencia_script IS 'Score de aderência ao script de vendas (0-100)';
COMMENT ON COLUMN feedback_ia.gestao_objecoes IS 'Score de gestão de objeções (0-100)';
COMMENT ON COLUMN feedback_ia.objecoes_identificadas IS 'Array de objeções identificadas na conversa';
COMMENT ON COLUMN feedback_ia.momentos_chave IS 'Array de momentos-chave da conversa com timestamps';

-- ============================================
-- TRIGGERS PARA ATUALIZAR atualizado_em
-- ============================================

-- Função para atualizar timestamp
CREATE OR REPLACE FUNCTION update_atualizado_em_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.atualizado_em = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger para empresa
DROP TRIGGER IF EXISTS update_empresa_atualizado_em ON empresa;
CREATE TRIGGER update_empresa_atualizado_em
    BEFORE UPDATE ON empresa
    FOR EACH ROW
    EXECUTE FUNCTION update_atualizado_em_column();

-- Trigger para usuario
DROP TRIGGER IF EXISTS update_usuario_atualizado_em ON usuario;
CREATE TRIGGER update_usuario_atualizado_em
    BEFORE UPDATE ON usuario
    FOR EACH ROW
    EXECUTE FUNCTION update_atualizado_em_column();

-- Trigger para cliente
DROP TRIGGER IF EXISTS update_cliente_atualizado_em ON cliente;
CREATE TRIGGER update_cliente_atualizado_em
    BEFORE UPDATE ON cliente
    FOR EACH ROW
    EXECUTE FUNCTION update_atualizado_em_column();

-- Trigger para gravacao_call
DROP TRIGGER IF EXISTS update_gravacao_call_atualizado_em ON gravacao_call;
CREATE TRIGGER update_gravacao_call_atualizado_em
    BEFORE UPDATE ON gravacao_call
    FOR EACH ROW
    EXECUTE FUNCTION update_atualizado_em_column();

-- ============================================
-- VIEWS ÚTEIS
-- ============================================

-- View: Estatísticas agregadas por empresa (usada pelo DashboardService)
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
GROUP BY e.id;

COMMENT ON VIEW v_estatisticas_empresa IS 'Estatísticas consolidadas por empresa - usada pelo DashboardService';

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
GROUP BY u.id, u.nome, u.email, e.id, e.nome_empresa;

COMMENT ON VIEW v_performance_vendedores IS 'Métricas de performance de cada vendedor';

-- View: Gravações completas com dados relacionados (JOINs pré-calculados)
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
LEFT JOIN feedback_ia f ON f.id_gravacao = g.id;

COMMENT ON VIEW v_gravacoes_completas IS 'Gravações com informações consolidadas de usuário, cliente, empresa e feedback';

-- ============================================
-- DADOS DE EXEMPLO (OPCIONAL)
-- Descomente para inserir dados de teste
-- ============================================

/*
-- Empresa de exemplo
INSERT INTO empresa (nome_empresa, cnpj, tipo_conta) 
VALUES ('Tech Sales Ltda', '12345678000190', 'PREMIUM')
ON CONFLICT (cnpj) DO NOTHING;

-- Usuário admin de exemplo (senha: admin123)
INSERT INTO usuario (id_empresa, nome, email, senha, funcao)
VALUES (
    (SELECT id FROM empresa WHERE cnpj = '12345678000190'),
    'Administrador',
    'admin@techsales.com',
    '$2a$10$XQz9Z9Z9Z9Z9Z9Z9Z9Z9Z.Z9Z9Z9Z9Z9Z9Z9Z9Z9Z9Z9Z9Z9Z9', -- Hash de 'admin123'
    'ADMIN'
)
ON CONFLICT (email) DO NOTHING;

-- Cliente de exemplo
INSERT INTO cliente (id_empresa, nome, cpf_cnpj, telefone, email, segmento)
VALUES (
    (SELECT id FROM empresa WHERE cnpj = '12345678000190'),
    'João Silva',
    '12345678900',
    '11987654321',
    'joao@exemplo.com',
    'Tecnologia'
)
ON CONFLICT DO NOTHING;
*/

-- ============================================
-- FIM DO SCRIPT
-- ============================================

COMMIT;
