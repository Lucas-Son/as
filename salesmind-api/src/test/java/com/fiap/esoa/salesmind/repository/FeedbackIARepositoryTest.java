package com.fiap.esoa.salesmind.repository;

import com.fiap.esoa.salesmind.enums.CategoriaAmbiental;
import com.fiap.esoa.salesmind.enums.Funcao;
import com.fiap.esoa.salesmind.enums.StatusProcessamento;
import com.fiap.esoa.salesmind.enums.StatusVenda;
import com.fiap.esoa.salesmind.model.FeedbackIA;
import com.fiap.esoa.salesmind.model.GravacaoCall;
import com.fiap.esoa.salesmind.model.Empresa;
import com.fiap.esoa.salesmind.model.Usuario;
import com.fiap.esoa.salesmind.model.Cliente;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração para FeedbackIARepository com PostgreSQL
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FeedbackIARepositoryTest {

    private static FeedbackIARepository repository;
    private static GravacaoCallRepository gravacaoRepository;
    private static EmpresaRepository empresaRepository;
    private static UsuarioRepository usuarioRepository;
    private static ClienteRepository clienteRepository;

    private static Long testFeedbackId;
    private static Long testGravacaoId;
    private static Long testEmpresaId;
    private static Long testUsuarioId;
    private static Long testClienteId;

    @BeforeAll
    static void setup() {
        repository = new FeedbackIARepository();
        gravacaoRepository = new GravacaoCallRepository();
        empresaRepository = new EmpresaRepository();
        usuarioRepository = new UsuarioRepository();
        clienteRepository = new ClienteRepository();

        Empresa empresa = new Empresa();
        empresa.setNomeEmpresa("Test Company");
        empresa.setCnpj("00000000000191");
        Empresa savedEmpresa = empresaRepository.save(empresa);
        testEmpresaId = savedEmpresa.getId();

        Usuario usuario = new Usuario();
        usuario.setIdEmpresa(testEmpresaId);
        usuario.setNome("Test User");
        usuario.setEmail("testuser@company.com");
        usuario.setSenha("password123");
        usuario.setFuncao(Funcao.VENDEDOR);
        Usuario savedUsuario = usuarioRepository.save(usuario);
        testUsuarioId = savedUsuario.getId();

        Cliente cliente = new Cliente();
        cliente.setIdEmpresa(testEmpresaId);
        cliente.setNome("Test Client");
        cliente.setCpfCnpj("12345678901");
        cliente.setTelefone("11999999999");
        Cliente savedCliente = clienteRepository.save(cliente);
        testClienteId = savedCliente.getId();

        GravacaoCall gravacao = new GravacaoCall();
        gravacao.setIdUsuario(testUsuarioId);
        gravacao.setIdCliente(testClienteId);
        gravacao.setAudioFilename("test-feedback-ia.mp3");
        gravacao.setAudioUrl("http://test.com/test-feedback-ia.mp3");
        gravacao.setStatusProcessamento(StatusProcessamento.CONCLUIDO);
        gravacao.setStatusVenda(StatusVenda.FECHADO);

        GravacaoCall savedGravacao = gravacaoRepository.save(gravacao);
        testGravacaoId = savedGravacao.getId();
    }

    @AfterAll
    static void cleanup() {
        if (testFeedbackId != null) {
            repository.deleteById(testFeedbackId);
        }
        if (testGravacaoId != null) {
            gravacaoRepository.deleteById(testGravacaoId);
        }
        if (testClienteId != null) {
            clienteRepository.deleteById(testClienteId);
        }
        if (testUsuarioId != null) {
            usuarioRepository.deleteById(testUsuarioId);
        }
        if (testEmpresaId != null) {
            empresaRepository.deleteById(testEmpresaId);
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. Inserir FeedbackIA com pontuações IA")
    void testInsertWithNewScores() {
        FeedbackIA feedback = new FeedbackIA();
        feedback.setIdGravacao(testGravacaoId);
        feedback.setIdEmpresa(testEmpresaId);

        feedback.setPontosFortes(Arrays.asList("Boa abertura", "Tom de voz adequado"));
        feedback.setPontosFracos(Arrays.asList("Não identificou necessidade", "Fechamento fraco"));
        feedback.setSugestoes(Arrays.asList("Fazer mais perguntas abertas", "Reforçar benefícios"));
        feedback.setSentimentScore(75);
        feedback.setProbabilidadeFechamento(60);
        feedback.setCategoriaAmbiental(CategoriaAmbiental.POSITIVO);

        feedback.setQualidadeAtendimento(82);
        feedback.setAderenciaScript(68);
        feedback.setGestaoObjecoes(55);
        feedback.setObjecoesIdentificadas(Arrays.asList(
                "Preço muito alto",
                "Já tenho fornecedor",
                "Preciso consultar sócios"));
        feedback.setMomentosChave(Arrays.asList(
                "00:15 - Apresentação inicial",
                "01:30 - Primeira objeção (preço)",
                "02:45 - Tentativa de fechamento",
                "03:20 - Segunda objeção (concorrente)",
                "04:10 - Fechamento sem sucesso"));

        FeedbackIA saved = repository.save(feedback);
        testFeedbackId = saved.getId();

        assertNotNull(saved.getId(), "ID deve ser gerado");
        assertEquals(82, saved.getQualidadeAtendimento(), "Pontuação de qualidade deve corresponder");
        assertEquals(68, saved.getAderenciaScript(), "Aderência ao script deve corresponder");
        assertEquals(55, saved.getGestaoObjecoes(), "Gestão de objeções deve corresponder");
        assertEquals(3, saved.getObjecoesIdentificadas().size(), "Deve ter 3 objeções");
        assertEquals(5, saved.getMomentosChave().size(), "Deve ter 5 momentos-chave");
    }

    @Test
    @Order(2)
    @DisplayName("2. Buscar por ID e verificar novos campos")
    void testFindByIdWithNewScores() {
        assertNotNull(testFeedbackId, "Feedback de teste deve existir");

        Optional<FeedbackIA> result = repository.findById(testFeedbackId);

        assertTrue(result.isPresent(), "Feedback deve ser encontrado");

        FeedbackIA feedback = result.get();

        assertEquals(82, feedback.getQualidadeAtendimento(), "Pontuação de qualidade deve ser persistida");
        assertEquals(68, feedback.getAderenciaScript(), "Pontuação de script deve ser persistida");
        assertEquals(55, feedback.getGestaoObjecoes(), "Pontuação de objeção deve ser persistida");
        assertNotNull(feedback.getObjecoesIdentificadas(), "Lista de objeções não deve ser null");
        assertEquals(3, feedback.getObjecoesIdentificadas().size(), "Deve ter 3 objeções");
        assertTrue(feedback.getObjecoesIdentificadas().contains("Preço muito alto"),
                "Deve conter 'Preço muito alto'");

        assertNotNull(feedback.getMomentosChave(), "Lista de momentos-chave não deve ser null");
        assertEquals(5, feedback.getMomentosChave().size(), "Deve ter 5 momentos-chave");
        assertTrue(feedback.getMomentosChave().get(0).startsWith("00:15"),
                "Primeiro momento deve ser às 00:15");
    }

    @Test
    @Order(3)
    @DisplayName("3. Atualizar feedback com pontuações IA modificadas")
    void testUpdateWithNewScores() {
        assertNotNull(testFeedbackId, "Feedback deve existir do teste anterior");

        Optional<FeedbackIA> result = repository.findById(testFeedbackId);
        assertTrue(result.isPresent(), "Feedback deve existir");

        FeedbackIA feedback = result.get();

        feedback.setQualidadeAtendimento(90);
        feedback.setAderenciaScript(85);
        feedback.setGestaoObjecoes(75);
        feedback.setObjecoesIdentificadas(Arrays.asList(
                "Preço muito alto",
                "Já tenho fornecedor"));
        feedback.setMomentosChave(Arrays.asList(
                "00:15 - Apresentação inicial",
                "01:30 - Objeção de preço bem gerenciada",
                "02:45 - Fechamento com sucesso"));

        repository.save(feedback);

        Optional<FeedbackIA> updated = repository.findById(testFeedbackId);
        assertTrue(updated.isPresent(), "Feedback atualizado deve existir");

        FeedbackIA updatedFeedback = updated.get();
        assertEquals(90, updatedFeedback.getQualidadeAtendimento(), "Qualidade deve ser atualizada");
        assertEquals(85, updatedFeedback.getAderenciaScript(), "Aderência ao script deve ser atualizada");
        assertEquals(75, updatedFeedback.getGestaoObjecoes(), "Gestão de objeções deve ser atualizada");
        assertEquals(2, updatedFeedback.getObjecoesIdentificadas().size(), "Deve ter 2 objeções");
        assertEquals(3, updatedFeedback.getMomentosChave().size(), "Deve ter 3 momentos-chave");
    }

    @Test
    @Order(4)
    @DisplayName("4. Inserir feedback com pontuações NULL (campos opcionais)")
    void testInsertWithNullScores() {
        GravacaoCall gravacao2 = new GravacaoCall();
        gravacao2.setIdUsuario(testUsuarioId);
        gravacao2.setIdCliente(testClienteId);
        gravacao2.setAudioFilename("test-null-scores.mp3");
        gravacao2.setAudioUrl("http://test.com/test-null-scores.mp3");
        gravacao2.setStatusProcessamento(StatusProcessamento.CONCLUIDO);
        GravacaoCall savedGravacao2 = gravacaoRepository.save(gravacao2);
        Long testGravacao2Id = savedGravacao2.getId();

        FeedbackIA feedback = new FeedbackIA();
        feedback.setIdGravacao(testGravacao2Id);
        feedback.setIdEmpresa(testEmpresaId);

        feedback.setPontosFortes(Arrays.asList("Teste"));
        feedback.setPontosFracos(Arrays.asList("Teste"));
        feedback.setSugestoes(Arrays.asList("Teste"));
        feedback.setSentimentScore(50);
        feedback.setProbabilidadeFechamento(50);
        feedback.setCategoriaAmbiental(CategoriaAmbiental.NEUTRO);

        FeedbackIA saved = repository.save(feedback);
        Long nullTestId = saved.getId();

        assertNotNull(saved.getId(), "ID deve ser gerado");
        assertNull(saved.getQualidadeAtendimento(), "Qualidade deve ser null");
        assertNull(saved.getAderenciaScript(), "Script deve ser null");
        assertNull(saved.getGestaoObjecoes(), "Objeção deve ser null");
        assertNotNull(saved.getObjecoesIdentificadas(), "Lista de objeções não deve ser null");
        assertEquals(0, saved.getObjecoesIdentificadas().size(), "Objeções deve estar vazia");
        assertNotNull(saved.getMomentosChave(), "Lista de momentos não deve ser null");
        assertEquals(0, saved.getMomentosChave().size(), "Momentos deve estar vazia");

        repository.deleteById(nullTestId);
        gravacaoRepository.deleteById(testGravacao2Id);
    }

    @Test
    @Order(5)
    @DisplayName("5. Testar constraints CHECK (pontuações devem ser 0-100)")
    void testCheckConstraints() {
        FeedbackIA feedback = new FeedbackIA();
        feedback.setIdGravacao(testGravacaoId);
        feedback.setIdEmpresa(testEmpresaId);
        feedback.setPontosFortes(Arrays.asList("Test"));
        feedback.setPontosFracos(Arrays.asList("Test"));
        feedback.setSugestoes(Arrays.asList("Test"));
        feedback.setSentimentScore(50);
        feedback.setProbabilidadeFechamento(50);
        feedback.setCategoriaAmbiental(CategoriaAmbiental.POSITIVO);

        feedback.setQualidadeAtendimento(150);

        assertThrows(RuntimeException.class, () -> {
            repository.save(feedback);
        }, "Deve lançar exceção para pontuação > 100");

        feedback.setQualidadeAtendimento(-10);

        assertThrows(RuntimeException.class, () -> {
            repository.save(feedback);
        }, "Deve lançar exceção para pontuação < 0");
    }

    @Test
    @Order(6)
    @DisplayName("6. Buscar todos e verificar novos campos na lista")
    void testFindAllWithNewScores() {
        List<FeedbackIA> allFeedbacks = repository.findAll();

        assertFalse(allFeedbacks.isEmpty(), "Deve ter pelo menos o feedback de teste");

        Optional<FeedbackIA> testFeedback = allFeedbacks.stream()
                .filter(f -> f.getId().equals(testFeedbackId))
                .findFirst();

        assertTrue(testFeedback.isPresent(), "Feedback de teste deve estar na lista");

        FeedbackIA feedback = testFeedback.get();
        assertEquals(90, feedback.getQualidadeAtendimento(), "Deve ter pontuação de qualidade atualizada");
        assertEquals(85, feedback.getAderenciaScript(), "Deve ter pontuação de script atualizada");
        assertEquals(75, feedback.getGestaoObjecoes(), "Deve ter pontuação de objeção atualizada");
    }

    @Test
    @Order(7)
    @DisplayName("7. CASCADE DELETE - Deletar gravação deleta feedback")
    void testCascadeDeleteGravacao() {
        GravacaoCall novaGravacao = new GravacaoCall();
        novaGravacao.setIdUsuario(testUsuarioId);
        novaGravacao.setIdCliente(testClienteId);
        novaGravacao.setAudioFilename("cascade-test.mp3");
        novaGravacao.setAudioUrl("http://test.com/cascade.mp3");
        novaGravacao.setStatusProcessamento(StatusProcessamento.CONCLUIDO);
        novaGravacao.setStatusVenda(StatusVenda.PENDENTE);
        GravacaoCall savedGravacao = gravacaoRepository.save(novaGravacao);
        Long gravacaoId = savedGravacao.getId();

        FeedbackIA feedback = new FeedbackIA();
        feedback.setIdGravacao(gravacaoId);
        feedback.setIdEmpresa(testEmpresaId);
        feedback.setPontosFortes(Arrays.asList("Teste"));
        feedback.setPontosFracos(Arrays.asList("Teste"));
        feedback.setSugestoes(Arrays.asList("Teste"));
        feedback.setSentimentScore(70);
        feedback.setProbabilidadeFechamento(50);
        feedback.setCategoriaAmbiental(CategoriaAmbiental.POSITIVO);
        feedback.setQualidadeAtendimento(75);
        feedback.setAderenciaScript(70);
        feedback.setGestaoObjecoes(65);
        feedback.setObjecoesIdentificadas(Arrays.asList("Teste"));
        feedback.setMomentosChave(Arrays.asList("00:00 - Teste"));
        
        FeedbackIA savedFeedback = repository.save(feedback);
        Long feedbackId = savedFeedback.getId();

        assertTrue(repository.existsById(feedbackId), "Feedback deve existir");
        assertTrue(gravacaoRepository.existsById(gravacaoId), "Gravação deve existir");

        gravacaoRepository.deleteById(gravacaoId);

        assertFalse(gravacaoRepository.existsById(gravacaoId), "Gravação deve ser deletada");
        assertFalse(repository.existsById(feedbackId), "Feedback deve ser deletado em cascata");
    }
}
