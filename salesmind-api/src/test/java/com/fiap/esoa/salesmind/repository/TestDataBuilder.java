package com.fiap.esoa.salesmind.repository;

import com.fiap.esoa.salesmind.enums.CategoriaAmbiental;
import com.fiap.esoa.salesmind.enums.Funcao;
import com.fiap.esoa.salesmind.enums.StatusProcessamento;
import com.fiap.esoa.salesmind.enums.StatusVenda;
import com.fiap.esoa.salesmind.model.*;

import java.util.Arrays;

/**
 * Helper para criação de dados de teste
 */
public class TestDataBuilder {

    private static EmpresaRepository empresaRepository = new EmpresaRepository();
    private static UsuarioRepository usuarioRepository = new UsuarioRepository();
    private static ClienteRepository clienteRepository = new ClienteRepository();
    private static GravacaoCallRepository gravacaoRepository = new GravacaoCallRepository();
    private static FeedbackIARepository feedbackRepository = new FeedbackIARepository();

    /**
     * Cria e persiste uma Empresa de teste
     */
    public static Empresa createEmpresa(String suffix) {
        Empresa empresa = new Empresa();
        empresa.setNomeEmpresa("Test Company " + suffix);
        empresa.setCnpj("000000000" + Math.abs(suffix.hashCode() % 100000));
        return empresaRepository.save(empresa);
    }

    /**
     * Cria e persiste um Usuario de teste vinculado à Empresa
     */
    public static Usuario createUsuario(Long idEmpresa, String suffix) {
        Usuario usuario = new Usuario();
        usuario.setIdEmpresa(idEmpresa);
        usuario.setNome("Test User " + suffix);
        usuario.setEmail("testuser" + suffix + "@company.com");
        usuario.setSenha("password123");
        usuario.setFuncao(Funcao.VENDEDOR);
        return usuarioRepository.save(usuario);
    }

    /**
     * Cria e persiste um Cliente de teste vinculado à Empresa
     */
    public static Cliente createCliente(Long idEmpresa, String suffix) {
        Cliente cliente = new Cliente();
        cliente.setIdEmpresa(idEmpresa);
        cliente.setNome("Test Client " + suffix);
        cliente.setCpfCnpj("123456" + String.format("%04d", Math.abs(suffix.hashCode() % 10000)));
        cliente.setTelefone("11999" + String.format("%06d", Math.abs(suffix.hashCode() % 1000000)));
        return clienteRepository.save(cliente);
    }

    /**
     * Cria e persiste uma GravacaoCall de teste vinculada a Usuario e Cliente
     */
    public static GravacaoCall createGravacao(Long idUsuario, Long idCliente, String suffix) {
        GravacaoCall gravacao = new GravacaoCall();
        gravacao.setIdUsuario(idUsuario);
        gravacao.setIdCliente(idCliente);
        gravacao.setAudioFilename("test-audio-" + suffix + ".mp3");
        gravacao.setAudioUrl("http://test.com/audio-" + suffix + ".mp3");
        gravacao.setStatusProcessamento(StatusProcessamento.CONCLUIDO);
        gravacao.setStatusVenda(StatusVenda.PENDENTE);
        return gravacaoRepository.save(gravacao);
    }

    /**
     * Cria e persiste um FeedbackIA de teste vinculado à GravacaoCall
     */
    public static FeedbackIA createFeedback(Long idGravacao, Long idEmpresa) {
        FeedbackIA feedback = new FeedbackIA();
        feedback.setIdGravacao(idGravacao);
        feedback.setIdEmpresa(idEmpresa);
        feedback.setPontosFortes(Arrays.asList("Ponto forte 1", "Ponto forte 2"));
        feedback.setPontosFracos(Arrays.asList("Ponto fraco 1"));
        feedback.setSugestoes(Arrays.asList("Sugestão 1", "Sugestão 2"));
        feedback.setSentimentScore(75);
        feedback.setProbabilidadeFechamento(60);
        feedback.setCategoriaAmbiental(CategoriaAmbiental.POSITIVO);
        feedback.setQualidadeAtendimento(80);
        feedback.setAderenciaScript(70);
        feedback.setGestaoObjecoes(65);
        feedback.setObjecoesIdentificadas(Arrays.asList("Objeção 1", "Objeção 2"));
        feedback.setMomentosChave(Arrays.asList("00:15 - Início", "01:30 - Objeção", "02:45 - Fechamento"));

        return feedbackRepository.save(feedback);
    }

    /**
     * Deleta dados de teste na ordem correta (respeitando chaves estrangeiras)
     */
    public static void cleanup(Long feedbackId, Long gravacaoId, Long clienteId, Long usuarioId, Long empresaId) {
        if (feedbackId != null) {
            try {
                feedbackRepository.deleteById(feedbackId);
            } catch (Exception ignored) {
            }
        }
        if (gravacaoId != null) {
            try {
                gravacaoRepository.deleteById(gravacaoId);
            } catch (Exception ignored) {
            }
        }
        if (clienteId != null) {
            try {
                clienteRepository.deleteById(clienteId);
            } catch (Exception ignored) {
            }
        }
        if (usuarioId != null) {
            try {
                usuarioRepository.deleteById(usuarioId);
            } catch (Exception ignored) {
            }
        }
        if (empresaId != null) {
            try {
                empresaRepository.deleteById(empresaId);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Configura conexão com banco de teste
     * Deve ser chamado ANTES de instanciar qualquer repository
     */
    public static void configureTestDatabase() {
        System.setProperty("DB_URL", "jdbc:postgresql://localhost:5432/salesmind_test");
        System.setProperty("DB_USER", "postgres");
        System.setProperty("DB_PASSWORD", "postgres");
    }

    /**
     * Limpa TODOS os dados do banco de teste
     * Deve ser chamado em @BeforeEach para garantir estado limpo
     */
    public static void cleanAllData() {
        try {
            // Delete all data in reverse FK order
            feedbackRepository.findAll().forEach(f -> feedbackRepository.deleteById(f.getId()));
            gravacaoRepository.findAll().forEach(g -> gravacaoRepository.deleteById(g.getId()));
            clienteRepository.findAll().forEach(c -> clienteRepository.deleteById(c.getId()));
            usuarioRepository.findAll().forEach(u -> usuarioRepository.deleteById(u.getId()));
            empresaRepository.findAll().forEach(e -> empresaRepository.deleteById(e.getId()));
        } catch (Exception e) {
            System.err.println("Erro ao limpar dados de teste: " + e.getMessage());
        }
    }
}
