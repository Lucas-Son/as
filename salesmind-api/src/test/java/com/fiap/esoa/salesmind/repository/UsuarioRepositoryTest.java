package com.fiap.esoa.salesmind.repository;

import com.fiap.esoa.salesmind.enums.Funcao;
import com.fiap.esoa.salesmind.model.Empresa;
import com.fiap.esoa.salesmind.model.Usuario;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração para UsuarioRepository com PostgreSQL
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UsuarioRepositoryTest {

    private static UsuarioRepository repository;
    private static EmpresaRepository empresaRepository;

    private Long testUsuarioId;
    private Long testEmpresaId;

    @BeforeAll
    static void setupAll() {
        TestDataBuilder.configureTestDatabase();
        repository = new UsuarioRepository();
        empresaRepository = new EmpresaRepository();
    }

    @BeforeEach
    void setup() {
        TestDataBuilder.cleanAllData();
        
        Empresa empresa = TestDataBuilder.createEmpresa(String.valueOf(System.currentTimeMillis()));
        testEmpresaId = empresa.getId();
    }

    @AfterEach
    void cleanup() {
        if (testUsuarioId != null) {
            try {
                repository.deleteById(testUsuarioId);
            } catch (Exception ignored) {
            }
            testUsuarioId = null;
        }
        if (testEmpresaId != null) {
            try {
                empresaRepository.deleteById(testEmpresaId);
            } catch (Exception ignored) {
            }
            testEmpresaId = null;
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. Inserir usuário e buscar por ID")
    void testInsertAndFindById() {
        Usuario usuario = TestDataBuilder.createUsuario(testEmpresaId, "ins");
        testUsuarioId = usuario.getId();

        assertNotNull(usuario.getId());
        assertEquals(testEmpresaId, usuario.getIdEmpresa());
        assertEquals("Test User ins", usuario.getNome());

        Optional<Usuario> found = repository.findById(testUsuarioId);
        assertTrue(found.isPresent());
        assertEquals("Test User ins", found.get().getNome());
    }

    @Test
    @Order(2)
    @DisplayName("2. Atualizar usuário")
    void testUpdate() {
        Usuario usuario = TestDataBuilder.createUsuario(testEmpresaId, "upd");
        testUsuarioId = usuario.getId();

        usuario.setNome("Updated User Name");
        usuario.setFuncao(Funcao.GERENTE);
        repository.save(usuario);

        Optional<Usuario> updated = repository.findById(testUsuarioId);
        assertTrue(updated.isPresent());
        assertEquals("Updated User Name", updated.get().getNome());
        assertEquals(Funcao.GERENTE, updated.get().getFuncao());
    }

    @Test
    @Order(3)
    @DisplayName("3. Buscar por email")
    void testFindByEmail() {
        Usuario usuario = TestDataBuilder.createUsuario(testEmpresaId, "email");
        testUsuarioId = usuario.getId();

        Optional<Usuario> found = repository.findByEmail(usuario.getEmail());
        assertTrue(found.isPresent());
        assertEquals(testUsuarioId, found.get().getId());

        Optional<Usuario> notFound = repository.findByEmail("nonexistent@email.com");
        assertFalse(notFound.isPresent());
    }

    @Test
    @Order(4)
    @DisplayName("4. Restrição UNIQUE em email")
    void testUniqueEmail() {
        Usuario usuario1 = TestDataBuilder.createUsuario(testEmpresaId, "uniq1");
        testUsuarioId = usuario1.getId();

        Usuario usuario2 = new Usuario();
        usuario2.setIdEmpresa(testEmpresaId);
        usuario2.setNome("Duplicate Email User");
        usuario2.setEmail(usuario1.getEmail());
        usuario2.setSenha("password");
        usuario2.setFuncao(Funcao.VENDEDOR);

        assertThrows(RuntimeException.class, () -> {
            repository.save(usuario2);
        }, "Deve lançar exceção para email duplicado");
    }

    @Test
    @Order(5)
    @DisplayName("5. Buscar por empresa")
    void testFindByEmpresa() {
        Usuario u1 = TestDataBuilder.createUsuario(testEmpresaId, "emp1");
        Usuario u2 = TestDataBuilder.createUsuario(testEmpresaId, "emp2");
        testUsuarioId = u1.getId();

        List<Usuario> usuarios = repository.findByEmpresa(testEmpresaId);

        assertTrue(usuarios.size() >= 2);
        assertTrue(usuarios.stream().anyMatch(u -> u.getId().equals(u1.getId())));
        assertTrue(usuarios.stream().anyMatch(u -> u.getId().equals(u2.getId())));

        repository.deleteById(u2.getId());
    }

    @Test
    @Order(6)
    @DisplayName("6. Restrição de chave estrangeira para empresa")
    void testForeignKeyConstraint() {
        Usuario usuario = new Usuario();
        usuario.setIdEmpresa(999999L);
        usuario.setNome("Invalid FK User");
        usuario.setEmail("invalidfk@test.com");
        usuario.setSenha("password");
        usuario.setFuncao(Funcao.VENDEDOR);

        assertThrows(RuntimeException.class, () -> {
            repository.save(usuario);
        }, "Deve lançar exceção para chave estrangeira inválida");
    }

    @Test
    @Order(7)
    @DisplayName("7. Deleta usuario")
    void testDelete() {
        Usuario usuario = TestDataBuilder.createUsuario(testEmpresaId, "del");
        Long id = usuario.getId();

        assertTrue(repository.existsById(id));

        repository.deleteById(id);

        assertFalse(repository.existsById(id));
        testUsuarioId = null;
    }

    @Test
    @Order(8)
    @DisplayName("8. Encontra todos usuários")
    void testFindAll() {
        Usuario u1 = TestDataBuilder.createUsuario(testEmpresaId, "all1");
        Usuario u2 = TestDataBuilder.createUsuario(testEmpresaId, "all2");
        testUsuarioId = u1.getId();

        List<Usuario> all = repository.findAll();

        assertTrue(all.size() >= 2);
        assertTrue(all.stream().anyMatch(u -> u.getId().equals(u1.getId())));

        repository.deleteById(u2.getId());
    }

    @Test
    @Order(9)
    @DisplayName("9. Contar usuários por empresa")
    void testCountByEmpresa() {
        Usuario u1 = TestDataBuilder.createUsuario(testEmpresaId, "count1");
        Usuario u2 = TestDataBuilder.createUsuario(testEmpresaId, "count2");
        testUsuarioId = u1.getId();

        long count = repository.countByEmpresa(testEmpresaId);
        assertTrue(count >= 2, "Deve contar pelo menos 2 usuários");

        repository.deleteById(u2.getId());
    }

    @Test
    @Order(10)
    @DisplayName("10. Buscar por função")
    void testFindByFuncao() {
        Usuario vendedor = TestDataBuilder.createUsuario(testEmpresaId, "vend");
        vendedor.setFuncao(Funcao.VENDEDOR);
        repository.save(vendedor);
        testUsuarioId = vendedor.getId();

        List<Usuario> vendedores = repository.findByFuncao(Funcao.VENDEDOR);
        assertTrue(vendedores.size() > 0);
        assertTrue(vendedores.stream().anyMatch(u -> u.getId().equals(testUsuarioId)));
    }
}
