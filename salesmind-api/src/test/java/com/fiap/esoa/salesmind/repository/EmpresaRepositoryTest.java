package com.fiap.esoa.salesmind.repository;

import com.fiap.esoa.salesmind.model.Empresa;
import com.fiap.esoa.salesmind.model.Usuario;
import com.fiap.esoa.salesmind.model.Cliente;
import com.fiap.esoa.salesmind.model.GravacaoCall;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração para EmpresaRepository com PostgreSQL
 * Testa operações CRUD, restrições UNIQUE e exclusões CASCADE
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EmpresaRepositoryTest {

    private static EmpresaRepository repository;
    private Long testEmpresaId;

    @BeforeAll
    static void setupAll() {
        TestDataBuilder.configureTestDatabase();
        repository = new EmpresaRepository();
    }

    @BeforeEach
    void setup() {
        TestDataBuilder.cleanAllData();
    }

    @AfterEach
    void cleanup() {
        if (testEmpresaId != null) {
            try {
                repository.deleteById(testEmpresaId);
            } catch (Exception ignored) {
            }
            testEmpresaId = null;
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. Inserir empresa e recuperar por ID")
    void testInsertAndFindById() {
        Empresa empresa = new Empresa();
        empresa.setNomeEmpresa("Test Company Insert");
        empresa.setCnpj("12345678000190");

        Empresa saved = repository.save(empresa);
        testEmpresaId = saved.getId();

        assertNotNull(saved.getId(), "ID deve ser gerado");
        assertEquals("Test Company Insert", saved.getNomeEmpresa());
        assertEquals("12345678000190", saved.getCnpj());
        assertNotNull(saved.getCriadoEm());

        Optional<Empresa> found = repository.findById(testEmpresaId);
        assertTrue(found.isPresent(), "Empresa deve ser encontrada");
        assertEquals("Test Company Insert", found.get().getNomeEmpresa());
    }

    @Test
    @Order(2)
    @DisplayName("2. Atualizar empresa")
    void testUpdate() {
        Empresa empresa = TestDataBuilder.createEmpresa("upd");
        testEmpresaId = empresa.getId();

        empresa.setNomeEmpresa("Updated Company Name");
        repository.save(empresa);

        Optional<Empresa> updated = repository.findById(testEmpresaId);
        assertTrue(updated.isPresent());
        assertEquals("Updated Company Name", updated.get().getNomeEmpresa());
    }

    @Test
    @Order(3)
    @DisplayName("3. Buscar por CNPJ")
    void testFindByCnpj() {
        Empresa empresa = TestDataBuilder.createEmpresa("cnpj");
        testEmpresaId = empresa.getId();

        Optional<Empresa> found = repository.findByCnpj(empresa.getCnpj());
        assertTrue(found.isPresent(), "Deve encontrar empresa por CNPJ");
        assertEquals(testEmpresaId, found.get().getId());
    }

    @Test
    @Order(4)
    @DisplayName("4. Restrição UNIQUE no CNPJ")
    void testUniqueCnpj() {
        Empresa empresa1 = TestDataBuilder.createEmpresa("uniq1");
        testEmpresaId = empresa1.getId();

        Empresa empresa2 = new Empresa();
        empresa2.setNomeEmpresa("Duplicate CNPJ Company");
        empresa2.setCnpj(empresa1.getCnpj());

        assertThrows(RuntimeException.class, () -> {
            repository.save(empresa2);
        }, "Deve lançar exceção para CNPJ duplicado");
    }

    @Test
    @Order(5)
    @DisplayName("5. Buscar todas as empresas")
    void testFindAll() {
        Empresa e1 = TestDataBuilder.createEmpresa("all1");
        Empresa e2 = TestDataBuilder.createEmpresa("all2");
        testEmpresaId = e1.getId(); // Will cleanup e1

        List<Empresa> all = repository.findAll();

        assertTrue(all.size() >= 2, "Deve ter pelo menos 2 empresas");
        assertTrue(all.stream().anyMatch(e -> e.getId().equals(e1.getId())));
        assertTrue(all.stream().anyMatch(e -> e.getId().equals(e2.getId())));

        repository.deleteById(e2.getId());
    }

    @Test
    @Order(6)
    @DisplayName("6. Excluir empresa")
    void testDelete() {
        Empresa empresa = TestDataBuilder.createEmpresa("del");
        Long id = empresa.getId();

        assertTrue(repository.existsById(id));

        repository.deleteById(id);

        assertFalse(repository.existsById(id));
        Optional<Empresa> notFound = repository.findById(id);
        assertFalse(notFound.isPresent());

        testEmpresaId = null;
    }

    @Test
    @Order(7)
    @DisplayName("7. Verificação de existência por ID")
    void testExistsById() {
        Empresa empresa = TestDataBuilder.createEmpresa("exists");
        testEmpresaId = empresa.getId();

        assertTrue(repository.existsById(testEmpresaId), "Deve existir");
        assertFalse(repository.existsById(999999L), "Não deve existir");
    }

    @Test
    @Order(8)
    @DisplayName("8. CASCADE DELETE - Deletar empresa deleta usuarios, clientes e gravacoes")
    void testCascadeDelete() {
        Empresa empresa = TestDataBuilder.createEmpresa("cascade");
        Long empresaId = empresa.getId();
        
        Usuario usuario = TestDataBuilder.createUsuario(empresaId, "cascade");
        Long usuarioId = usuario.getId();
        
        Cliente cliente = TestDataBuilder.createCliente(empresaId, "cascade");
        Long clienteId = cliente.getId();
        
        GravacaoCall gravacao = TestDataBuilder.createGravacao(usuarioId, clienteId, "cascade");
        Long gravacaoId = gravacao.getId();

        assertTrue(repository.existsById(empresaId));
        assertTrue(new UsuarioRepository().existsById(usuarioId));
        assertTrue(new ClienteRepository().existsById(clienteId));
        assertTrue(new GravacaoCallRepository().existsById(gravacaoId));

        repository.deleteById(empresaId);

        assertFalse(repository.existsById(empresaId), "Empresa deve ser deletada");
        assertFalse(new UsuarioRepository().existsById(usuarioId), "Usuario deve ser deletado em cascata");
        assertFalse(new ClienteRepository().existsById(clienteId), "Cliente deve ser deletado em cascata");
        assertFalse(new GravacaoCallRepository().existsById(gravacaoId), "Gravacao deve ser deletada em cascata");
        
        testEmpresaId = null;
    }
}
