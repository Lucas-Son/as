package com.fiap.esoa.salesmind.repository;

import com.fiap.esoa.salesmind.enums.StatusProcessamento;
import com.fiap.esoa.salesmind.enums.StatusVenda;
import com.fiap.esoa.salesmind.model.*;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração para GravacaoCallRepository com PostgreSQL
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GravacaoCallRepositoryTest {

    private static GravacaoCallRepository repository;
    private Long testGravacaoId;
    private Long testEmpresaId;
    private Long testUsuarioId;
    private Long testClienteId;

    @BeforeAll
    static void setupAll() {
        TestDataBuilder.configureTestDatabase();
        repository = new GravacaoCallRepository();
    }

    @BeforeEach
    void setup() {
        TestDataBuilder.cleanAllData();
        Empresa empresa = TestDataBuilder.createEmpresa(String.valueOf(System.currentTimeMillis()));
        testEmpresaId = empresa.getId();

        Usuario usuario = TestDataBuilder.createUsuario(testEmpresaId, String.valueOf(System.currentTimeMillis()));
        testUsuarioId = usuario.getId();

        Cliente cliente = TestDataBuilder.createCliente(testEmpresaId, String.valueOf(System.currentTimeMillis()));
        testClienteId = cliente.getId();
    }

    @AfterEach
    void cleanup() {
        TestDataBuilder.cleanup(null, testGravacaoId, testClienteId, testUsuarioId, testEmpresaId);
        testGravacaoId = null;
        testUsuarioId = null;
        testClienteId = null;
        testEmpresaId = null;
    }

    @Test
    @Order(1)
    @DisplayName("1. Inserir gravação e buscar por ID")
    void testInsertAndFindById() {
        GravacaoCall gravacao = TestDataBuilder.createGravacao(testUsuarioId, testClienteId, "ins");
        testGravacaoId = gravacao.getId();

        assertNotNull(gravacao.getId());
        assertEquals(testUsuarioId, gravacao.getIdUsuario());
        assertEquals(testClienteId, gravacao.getIdCliente());
        assertEquals(StatusProcessamento.CONCLUIDO, gravacao.getStatusProcessamento());

        Optional<GravacaoCall> found = repository.findById(testGravacaoId);
        assertTrue(found.isPresent());
        assertEquals("test-audio-ins.mp3", found.get().getAudioFilename());
    }

    @Test
    @Order(2)
    @DisplayName("2. Atualizar gravação com enums")
    void testUpdateWithEnums() {
        GravacaoCall gravacao = TestDataBuilder.createGravacao(testUsuarioId, testClienteId, "upd");
        testGravacaoId = gravacao.getId();

        gravacao.setStatusVenda(StatusVenda.FECHADO);
        gravacao.setStatusProcessamento(StatusProcessamento.CONCLUIDO);
        gravacao.setTranscricao("Transcription text here");
        repository.save(gravacao);

        Optional<GravacaoCall> updated = repository.findById(testGravacaoId);
        assertTrue(updated.isPresent());
        assertEquals(StatusVenda.FECHADO, updated.get().getStatusVenda());
        assertEquals(StatusProcessamento.CONCLUIDO, updated.get().getStatusProcessamento());
        assertEquals("Transcription text here", updated.get().getTranscricao());
    }

    @Test
    @Order(3)
    @DisplayName("3. Buscar por status processamento")
    void testFindByStatusProcessamento() {
        GravacaoCall g1 = TestDataBuilder.createGravacao(testUsuarioId, testClienteId, "st1");
        g1.setStatusProcessamento(StatusProcessamento.PROCESSANDO);
        repository.save(g1);
        testGravacaoId = g1.getId();

        List<GravacaoCall> found = repository.findByStatusProcessamento(StatusProcessamento.PROCESSANDO);
        assertFalse(found.isEmpty());
        assertTrue(found.stream().anyMatch(g -> g.getId().equals(testGravacaoId)));
    }

    @Test
    @Order(4)
    @DisplayName("4. Buscar por vendedor (usuário)")
    void testFindByVendedor() {
        GravacaoCall g1 = TestDataBuilder.createGravacao(testUsuarioId, testClienteId, "vend1");
        GravacaoCall g2 = TestDataBuilder.createGravacao(testUsuarioId, testClienteId, "vend2");
        testGravacaoId = g1.getId();

        List<GravacaoCall> gravacoes = repository.findByUsuario(testUsuarioId);

        assertTrue(gravacoes.size() >= 2);
        assertTrue(gravacoes.stream().anyMatch(g -> g.getId().equals(g1.getId())));
        assertTrue(gravacoes.stream().anyMatch(g -> g.getId().equals(g2.getId())));

        repository.deleteById(g2.getId());
    }

    @Test
    @Order(5)
    @DisplayName("5. Buscar por cliente")
    void testFindByCliente() {
        GravacaoCall g1 = TestDataBuilder.createGravacao(testUsuarioId, testClienteId, "cli1");
        GravacaoCall g2 = TestDataBuilder.createGravacao(testUsuarioId, testClienteId, "cli2");
        testGravacaoId = g1.getId();

        List<GravacaoCall> gravacoes = repository.findByCliente(testClienteId);

        assertTrue(gravacoes.size() >= 2);
        assertTrue(gravacoes.stream().anyMatch(g -> g.getId().equals(g1.getId())));

        repository.deleteById(g2.getId());
    }

    @Test
    @Order(6)
    @DisplayName("6. Campos anuláveis (transcricao, resumoIA, duracaoSegundos)")
    void testNullableFields() {
        GravacaoCall gravacao = TestDataBuilder.createGravacao(testUsuarioId, testClienteId, "null");
        testGravacaoId = gravacao.getId();

        Optional<GravacaoCall> found = repository.findById(testGravacaoId);
        assertTrue(found.isPresent());
        assertNull(found.get().getTranscricao());
        assertNull(found.get().getResumoIA());
        assertNull(found.get().getDuracaoSegundos());

        gravacao.setTranscricao("Test transcription");
        gravacao.setResumoIA("Test summary");
        gravacao.setDuracaoSegundos(120);
        repository.save(gravacao);

        Optional<GravacaoCall> updated = repository.findById(testGravacaoId);
        assertTrue(updated.isPresent());
        assertEquals("Test transcription", updated.get().getTranscricao());
        assertEquals("Test summary", updated.get().getResumoIA());
        assertEquals(120, updated.get().getDuracaoSegundos());
    }

    @Test
    @Order(7)
    @DisplayName("7. Restrições de chave estrangeira")
    void testForeignKeyConstraints() {
        GravacaoCall gravacao = new GravacaoCall();
        gravacao.setIdUsuario(999999L);
        gravacao.setIdCliente(testClienteId);
        gravacao.setAudioFilename("invalid.mp3");
        gravacao.setAudioUrl("http://test.com/invalid.mp3");

        assertThrows(RuntimeException.class, () -> {
            repository.save(gravacao);
        }, "Should throw exception for invalid usuario FK");
    }

    @Test
    @Order(8)
    @DisplayName("8. Buscar todas gravações")
    void testGetAll() {
        GravacaoCall g1 = TestDataBuilder.createGravacao(testUsuarioId, testClienteId, "all1");
        GravacaoCall g2 = TestDataBuilder.createGravacao(testUsuarioId, testClienteId, "all2");
        testGravacaoId = g1.getId();

        List<GravacaoCall> all = repository.findAll();

        assertTrue(all.size() >= 2);
        assertTrue(all.stream().anyMatch(g -> g.getId().equals(g1.getId())));

        repository.deleteById(g2.getId());
    }

    @Test
    @Order(9)
    @DisplayName("9. Contar gravações por empresa")
    void testCountByEmpresa() {
        GravacaoCall g1 = TestDataBuilder.createGravacao(testUsuarioId, testClienteId, "count1");
        GravacaoCall g2 = TestDataBuilder.createGravacao(testUsuarioId, testClienteId, "count2");
        testGravacaoId = g1.getId();

        GravacaoCallRepository gravacaoRepo = new GravacaoCallRepository();
        long count = gravacaoRepo.countByEmpresa(testEmpresaId);
        assertTrue(count >= 2, "Deve contar pelo menos 2 gravações");

        repository.deleteById(g2.getId());
    }

    @Test
    @Order(10)
    @DisplayName("10. Contar vendas fechadas por usuário")
    void testCountVendasFechadasByUsuario() {
        GravacaoCall g1 = TestDataBuilder.createGravacao(testUsuarioId, testClienteId, "venda1");
        g1.setStatusVenda(StatusVenda.FECHADO);
        repository.save(g1);
        
        GravacaoCall g2 = TestDataBuilder.createGravacao(testUsuarioId, testClienteId, "venda2");
        g2.setStatusVenda(StatusVenda.FECHADO);
        repository.save(g2);
        
        testGravacaoId = g1.getId();

        long count = repository.countVendasFechadasByUsuario(testUsuarioId);
        assertTrue(count >= 2, "Deve contar pelo menos 2 vendas fechadas");

        repository.deleteById(g2.getId());
    }
}
